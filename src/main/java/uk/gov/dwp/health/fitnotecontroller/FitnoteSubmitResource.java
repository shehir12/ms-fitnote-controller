package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.BarcodeContents;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.Views;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageHashException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageTransformException;
import uk.gov.dwp.health.fitnotecontroller.utils.ImageCompressor;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import uk.gov.dwp.health.fitnotecontroller.utils.MemoryChecker;
import uk.gov.dwp.health.fitnotecontroller.utils.OcrChecker;
import uk.gov.dwp.health.fitnotecontroller.utils.PdfImageExtractor;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Path("/")
public class FitnoteSubmitResource {
    private static final Logger LOG = LoggerFactory.getLogger(FitnoteSubmitResource.class.getName());
    private static final String LOG_STANDARD_REGEX = "[\\u0000-\\u001f]";
    private static final String ERROR_MSG = "Unable to process request";
    private FitnoteControllerConfiguration controllerConfiguration;
    private ImageCompressor imageCompressor;
    private ImageStorage imageStorage;
    private JsonValidator validator;
    private OcrChecker ocrChecker;

    public FitnoteSubmitResource(FitnoteControllerConfiguration controllerConfiguration, ImageStorage imageStorage) {
        this(controllerConfiguration, new JsonValidator(), new OcrChecker(controllerConfiguration), imageStorage, new ImageCompressor(controllerConfiguration));
    }

    public FitnoteSubmitResource(FitnoteControllerConfiguration controllerConfiguration, JsonValidator validator, OcrChecker ocrChecker, ImageStorage imageStorage, ImageCompressor imageCompressor) {
        this.controllerConfiguration = controllerConfiguration;
        this.validator = validator;
        this.ocrChecker = ocrChecker;
        this.imageStorage = imageStorage;
        this.imageCompressor = imageCompressor;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/imagestatus")
    public Response checkFitnote(@QueryParam("sessionId") Optional<String> sessionId) throws ImagePayloadException, IOException, CryptoException {
        Response response;
        if (sessionId.isPresent()) {
            String formatted = sessionId.get().replaceAll(LOG_STANDARD_REGEX, "");
            ImagePayload payload = imageStorage.getPayload(formatted);
            
            response = Response.status(Response.Status.OK).entity(createStatusOnlyResponseFrom(payload.getFitnoteCheckStatus(), payload.getBarcodeCheckStatus())).build();
        } else {
            response = Response.status(Response.Status.BAD_REQUEST).build();

        }
        return response;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/photo")
    public Response submitFitnote(String json) {
        ImagePayload incomingPayload;
        Response response;
        try {

            if (MemoryChecker.hasEnoughMemoryForRequest(Runtime.getRuntime(), controllerConfiguration.getEstimatedRequestMemoryMb())) {
                incomingPayload = validator.validateAndTranslateSubmission(json.replaceAll(LOG_STANDARD_REGEX, ""));
                imageStorage.updateImageHashStore(incomingPayload.getImage());

                ImagePayload storedPayload = imageStorage.getPayload(incomingPayload.getSessionId());
                storedPayload.setFitnoteCheckStatus(incomingPayload.getFitnoteCheckStatus());
                storedPayload.setImage(incomingPayload.getImage());
                imageStorage.updateImageDetails(storedPayload);

                response = createResponseOf(HttpStatus.SC_ACCEPTED, createSessionOnlyResponseFrom(incomingPayload));
                LOG.debug("Json Validated correctly");
                checkAsynchronously(storedPayload);

            } else {
                response = createResponseOf(HttpStatus.SC_SERVICE_UNAVAILABLE, ERROR_MSG);
            }

        } catch (ImagePayloadException | ImageHashException | CryptoException e) {
            response = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_MSG);
            formatAndLogError(e.getClass().getName(), e.getMessage());
            LOG.debug(ERROR_MSG, e);
        } catch (IOException e) {
            response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_MSG);
            LOG.error("IOException :: {}", e.getClass().getName(), e.getMessage());
            LOG.debug(ERROR_MSG, e);
        }
        LOG.debug("Completed /photo, send back status {}", response.getStatusInfo().getStatusCode());
        return response;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/qrcode")
    public Response submitBarcode(String json) {
        ImagePayload incomingPayload;
        Response response;
        try {

            if (MemoryChecker.hasEnoughMemoryForRequest(Runtime.getRuntime(), controllerConfiguration.getEstimatedRequestMemoryMb())) {
                incomingPayload = validator.validateAndTranslateBarcodeSubmission(json.replaceAll(LOG_STANDARD_REGEX, ""));
                ImagePayload storedPayload = imageStorage.getPayload(incomingPayload.getSessionId());
                storedPayload.setBarcodeCheckStatus(incomingPayload.getBarcodeCheckStatus());
                storedPayload.setBarcodeImage(incomingPayload.getBarcodeImage());
                imageStorage.updateQrCodeDetails(storedPayload);

                response = createResponseOf(HttpStatus.SC_ACCEPTED, createSessionOnlyResponseFrom(incomingPayload));
                LOG.debug("Json validated correctly");
                checkBarcodeAsynchronously(storedPayload);

            } else {
                response = createResponseOf(HttpStatus.SC_SERVICE_UNAVAILABLE, ERROR_MSG);
            }

        } catch (ImagePayloadException e) {
            response = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_MSG);
            LOG.error("ImagePayloadException :: {}", e.getMessage());
            LOG.debug(ERROR_MSG, e);
        } catch (CryptoException e) {
            response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_MSG);
            LOG.error("CryptoException :: {}", e.getMessage());
            LOG.debug(ERROR_MSG, e);
        } catch (IOException e) {
            response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_MSG);
            LOG.error("JsonProcessingException :: {}", e.getMessage());
            LOG.debug(ERROR_MSG, e);
        }

        return response;
    }

    private void checkBarcodeAsynchronously(ImagePayload payload) {
        payload.setBarcodeCheckStatus(ImagePayload.Status.CHECKING);
        new Thread(() -> {
            try {
                if (payload.getBarcodeImage() == null) {
                    throw new ImagePayloadException("No Barcode Image found");
                }

                decodeBarcodeContents(payload);

            } catch (ImagePayloadException | IOException | OutOfMemoryError | CryptoException e) {
                payload.setBarcodeCheckStatus(ImagePayload.Status.FAILED_ERROR);
                payload.setBarcodeImage(null);

                LOG.error("{} :: {}", e.getClass().getName(), e.getMessage());
                LOG.debug(e.getClass().getName(), e);
            }

        }).start();
    }

    private void formatAndLogError(String className, String message) {
        LOG.error("{} {}", className, message);
    }

    private void decodeBarcodeContents(ImagePayload payload) throws IOException, ImagePayloadException, CryptoException {
        LOG.info("Attempting to decode Barcode Code");
        BarcodeContents barcodeContents = new BarcodeAnalyser(payload.getBarcodeImage()).decodeBarcodeContents();

        if (barcodeContents == null) {
            LOG.info("Unable to decode Barcode code");
            payload.setBarcodeCheckStatus(ImagePayload.Status.FAILED_IMG_BARCODE);
            payload.setBarcodeImage(null);

        } else {
            LOG.info("Barcode code successfully decoded");
            payload.setBarcodeCheckStatus(ImagePayload.Status.PASS_IMG_BARCODE);
            payload.setBarcodeCheckStatus(ImagePayload.Status.SUCCEEDED);
        }

        imageStorage.updateQrCodeDetails(payload);
    }

    private void checkAsynchronously(ImagePayload payload) {
        payload.setFitnoteCheckStatus(ImagePayload.Status.CHECKING);
        new Thread(() -> {
            try {
                if (!validateAndOcrImageFromInputTypes(payload)) {
                    imageStorage.updateImageDetails(payload);
                    return;
                }

                byte[] compressedImage;
                try (ByteArrayInputStream imageStream = new ByteArrayInputStream(Base64.decodeBase64(payload.getImage()))) {
                    compressedImage = imageCompressor.compressBufferedImage(ImageIO.read(imageStream), controllerConfiguration.getTargetImageSizeKB(), controllerConfiguration.isGreyScale());
                }
                validateCompressedImage(compressedImage, payload, false);
                imageStorage.updateImageDetails(payload);

            } catch (Exception e) {
                payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_ERROR);
                payload.setImage(null);

                formatAndLogError(e.getClass().getName(), e.getMessage());
                LOG.debug(e.getClass().getName(), e);

                try {
                    imageStorage.updateImageDetails(payload);

                } catch (ImagePayloadException | IOException | CryptoException e1) {
                    formatAndLogError(e1.getClass().getName(), e1.getMessage());
                    LOG.debug(e1.getClass().getName(), e1);
                }
            }
        }).start();
    }

    private String createSessionOnlyResponseFrom(ImagePayload payload) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        return mapper.writerWithView(Views.SessionOnly.class).writeValueAsString(payload);
    }

    private String createStatusOnlyResponseFrom(ImagePayload.Status fitnoteStatus, ImagePayload.Status barcodeStatus) {
        return String.format("{\"fitnoteStatus\":\"%s\", \"barcodeStatus\" : \"%s\"}", fitnoteStatus, barcodeStatus);
    }

    private Response createResponseOf(int status, String message) {
        return Response.status(status).entity(message).build();
    }

    private boolean validateAndOcrImageFromInputTypes(ImagePayload payload) throws IOException, ImagePayloadException, ImageCompressException {
        boolean validationStatus = false;
        try {
            if (validatePayloadImageJpg(payload)) {
                validationStatus = ocrImage(payload);
            }

        } catch (ImageTransformException e) {
            LOG.debug(e.getClass().getName(), e);

            byte[] pdfImage = PdfImageExtractor.extractImage(Base64.decodeBase64(payload.getImage()), controllerConfiguration.getPdfScanDPI());
            if (pdfImage != null) {
                try (ByteArrayInputStream pdfStream = new ByteArrayInputStream(pdfImage)) {
                    if (validateLandscapeImage(payload, ImageIO.read(pdfStream))) {
                        payload.setImage(Base64.encodeBase64String(pdfImage));
                        validationStatus = ocrImage(payload);
                    }
                }

            } else {
                throw new ImagePayloadException("The encoded string could not be transformed to a BufferedImage");
            }
        }

        return validationStatus;
    }

    private boolean validatePayloadImageJpg(ImagePayload payload) throws IOException, ImagePayloadException, ImageTransformException {
        if (payload.getImage() == null) {
            throw new ImagePayloadException("The encoded string is null.  Cannot be transformed to an image");
        }

        BufferedImage imageBuf;
        try (ByteArrayInputStream imageStream = new ByteArrayInputStream(Base64.decodeBase64(payload.getImage()))) {
            imageBuf = ImageIO.read(imageStream);
        }

        if (imageBuf == null) {
            throw new ImageTransformException("The encoded string could not be transformed to a BufferedImage");
        }

        return validateLandscapeImage(payload, imageBuf);
    }

    private boolean validateLandscapeImage(ImagePayload payload, BufferedImage imageBuf) {
        boolean returnValue = false;
        if (controllerConfiguration.isLandscapeImageEnforced()) {
            if (imageBuf.getHeight() > imageBuf.getWidth()) {
                LOG.error("Image is not landscape (H:{}, W:{})", imageBuf.getHeight(), imageBuf.getWidth());
                payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_SIZE);
            }
        } else {
            payload.setFitnoteCheckStatus(ImagePayload.Status.PASS_IMG_SIZE);
            LOG.info("NO LANDSCAPE ENFORCEMENT FOR IMAGE DIMENSIONS");
            returnValue = true;
        }
        return returnValue;
    }

    private boolean validateCompressedImage(byte[] compressedImage, ImagePayload payload, boolean validSize) throws ImageCompressException {
        boolean returnValue = false;
        if (compressedImage == null) {
            throw new ImageCompressException("The compressed image return a null byte array");
        }

        if (validSize) {
            if (compressedImage.length >= (controllerConfiguration.getTargetImageSizeKB() * 1000)) {
                LOG.info("Written {} bytes back to the ImagePayload for {} - OCR Checks", compressedImage.length, payload.getSessionId());
                payload.setImage(Base64.encodeBase64String(compressedImage));
                returnValue = true;
            } else {
                payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_SIZE);
                LOG.info("Image failed at validateCompressedImage. Threshold is {}, actual size is {}", controllerConfiguration.getTargetImageSizeKB() * 1000, compressedImage.length);
            }
        }
        if (!validSize) {
            LOG.info("Written {} bytes back to the ImagePayload for {} - FINAL Submission", compressedImage.length, payload.getSessionId());
            payload.setFitnoteCheckStatus(ImagePayload.Status.SUCCEEDED);
            payload.setImage(Base64.encodeBase64String(compressedImage));
            returnValue = true;
        }
        return returnValue;
    }


    private boolean ocrImage(ImagePayload payload) throws IOException, ImageCompressException {
        boolean ocrStatus = false;

        if (!controllerConfiguration.isOcrChecksEnabled()) {
            LOG.info("NO OCR CHECKS OR ROTATION CONFIGURED IMAGES");
            ocrStatus = true;

        } else {
            byte[] compressedImage;
            try (ByteArrayInputStream imageStream = new ByteArrayInputStream(Base64.decodeBase64(payload.getImage()))) {
                compressedImage = imageCompressor.compressBufferedImage(ImageIO.read(imageStream), controllerConfiguration.getScanTargetImageSizeKb(), false);
            }

            if (validateCompressedImage(compressedImage, payload, true)) {
                ExpectedFitnoteFormat.Status imageStatus = ocrChecker.imageContainsReadableText(payload);
                if (imageStatus.equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
                    payload.setFitnoteCheckStatus(ImagePayload.Status.PASS_IMG_OCR);
                    ocrStatus = true;

                } else if (imageStatus.equals(ExpectedFitnoteFormat.Status.PARTIAL)) {
                    payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_OCR_PARTIAL);
                    payload.setImage(null);

                } else if ((imageStatus.equals(ExpectedFitnoteFormat.Status.FAILED)) || (imageStatus.equals(ExpectedFitnoteFormat.Status.INITIALISED))) {
                    payload.setFitnoteCheckStatus(ImagePayload.Status.FAILED_IMG_OCR);
                    payload.setImage(null);

                    LOG.warn("Unable to OCR the fitnote");
                }
            }
        }

        return ocrStatus;
    }
}