package fitnotecontroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import fitnotecontroller.application.FitnoteControllerConfiguration;
import fitnotecontroller.domain.ExpectedFitnoteFormat;
import fitnotecontroller.domain.ImagePayload;
import fitnotecontroller.domain.StatusItem;
import fitnotecontroller.exception.ImageCompressException;
import fitnotecontroller.exception.ImagePayloadException;
import fitnotecontroller.utils.ImageCompressor;
import fitnotecontroller.utils.JsonValidator;
import fitnotecontroller.utils.MemoryChecker;
import fitnotecontroller.utils.OcrChecker;
import gherkin.deps.net.iharder.Base64;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("squid:S2925")
@RunWith(MockitoJUnitRunner.class)
public class FitnoteSubmitResourceTest {

    private static final String SESSION = "session1";
    private Optional sessionIdParameter = Mockito.mock(Optional.class);
    private long EXPIRY_TIME_MILLISECONDS = 15000;
    private static String LANDSCAPE_FITNOTE_IMAGE;
    private static String PORTRAIT_FITNOTE_IMAGE;
    private static String PDF_FITNOTE_IMAGE;
    private static String FITNOTE_QR_TEST;
    private static String PORTRAIT_JSON;
    private static String VALID_JSON;
    private static String PDF_JSON;
    private static String QR_JSON;
    private int OVER_MAX_MEMORY;

    private FitnoteSubmitResource resourceUnderTest;
    private ImageStorage imageStorage;

    @Mock
    private JsonValidator validator;

    @Mock
    private OcrChecker ocrChecker;

    @Mock
    private FitnoteControllerConfiguration controllerConfiguration;

    @Mock
    private ImageCompressor imageCompressor;

    @Before
    public void setup() throws IOException, ImagePayloadException, ImageCompressException {
        when(imageCompressor.compressBufferedImage(any(BufferedImage.class), eq(3), eq(false))).thenReturn(new byte[3000]);
        when(imageCompressor.compressBufferedImage(any(BufferedImage.class), eq(2), eq(true))).thenReturn(new byte[2000]);
        when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(3);
        when(controllerConfiguration.getScanTargetImageSizeKb()).thenReturn(3);
        when(controllerConfiguration.getTargetImageSizeKB()).thenReturn(2);
        when(controllerConfiguration.isGreyScale()).thenReturn(true);
        imageStorage = new ImageStorage(controllerConfiguration);

        resourceUnderTest = new FitnoteSubmitResource(controllerConfiguration, validator, ocrChecker, imageStorage, imageCompressor);

        LANDSCAPE_FITNOTE_IMAGE = Base64.encodeFromFile("src/test/resources/FullPage_Landscape.jpg");
        PORTRAIT_FITNOTE_IMAGE = Base64.encodeFromFile("src/test/resources/FullPage_Portrait.jpg");
        PDF_FITNOTE_IMAGE = Base64.encodeFromFile("src/test/resources/FullPage_Portrait.pdf");
        FITNOTE_QR_TEST = Base64.encodeFromFile("src/test/resources/working_barcode.jpg");

        PORTRAIT_JSON = "{\"image\":\"" + PORTRAIT_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
        VALID_JSON = "{\"image\":\"" + LANDSCAPE_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
        QR_JSON = "{\"barcodeImage\":\"" + FITNOTE_QR_TEST + "\",\"sessionId\":\"" + SESSION + "\"}";
        PDF_JSON = "{\"image\":\"" + PDF_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";

        when(sessionIdParameter.isPresent()).thenReturn(true);
        when(sessionIdParameter.get()).thenReturn(SESSION);
        imageStorage.getPayload(SESSION);

        long freeMemory = MemoryChecker.returnCurrentAvailableMemoryInMb(Runtime.getRuntime());
        OVER_MAX_MEMORY = (int) freeMemory + 300;
        //The cast is a little ugly as theoretically could loose data, but ok for this purpose.
    }

    @Test
    public void checkFitnoteCallFailsWhenNoSessionIdParameterExists() throws ImagePayloadException {
        Optional<String> missingSessionIdParameter = Mockito.mock(Optional.class);
        when(missingSessionIdParameter.isPresent()).thenReturn(false);
        Response response = resourceUnderTest.checkFitnote(missingSessionIdParameter);
        assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
    }

    @Test
    public void checkFitnoteCallIsOkWithUnknownSessionIdParameterIsPassedIn() throws ImagePayloadException {
        when(sessionIdParameter.isPresent()).thenReturn(true);
        when(sessionIdParameter.get()).thenReturn("Unknown session id");
        Response response = resourceUnderTest.checkFitnote(sessionIdParameter);
        assertThat(response.getStatus(), is(equalTo(SC_OK)));
    }

    @Test
    public void checkFitnoteCallFailsWhenNoSessionIdParameterIsPassedIn() throws ImagePayloadException {
        when(sessionIdParameter.isPresent()).thenReturn(false);
        Response response = resourceUnderTest.checkFitnote(sessionIdParameter);
        assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
    }

    @Test
    public void portraitImageFailsChecksWhenOn() throws IOException, ImagePayloadException, InterruptedException {
        when(controllerConfiguration.isLandscapeImageEnforced()).thenReturn(true);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(PORTRAIT_FITNOTE_IMAGE);
        imagePayload.setSessionId(SESSION);
        createAndValidateImage(PORTRAIT_JSON, true, imagePayload);
        Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
        verify(validator).validateAndTranslateSubmission(PORTRAIT_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
        long startTime = System.currentTimeMillis();

        response = resourceUnderTest.checkFitnote(sessionIdParameter);
        while (((System.currentTimeMillis() - startTime) < EXPIRY_TIME_MILLISECONDS) && (!decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("FAILED_IMG_SIZE"))) {
            assertThat(response.getStatus(), is(equalTo(SC_OK)));

            Thread.sleep(1000);
            response = resourceUnderTest.checkFitnote(sessionIdParameter);
        }

        assertThat(decodeResponse(response.getEntity().toString()).getFitnoteStatus(), is("FAILED_IMG_SIZE"));
    }

    @Test
    public void failsWhenThereIsNotEnoughMemoryForPhoto() throws ImagePayloadException {
        when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(OVER_MAX_MEMORY);

        Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_SERVICE_UNAVAILABLE)));
    }

    @Test
    public void failsWhenThereIsNotEnoughMemoryForBarcode() throws ImagePayloadException {
        when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(OVER_MAX_MEMORY);

        Response response = resourceUnderTest.submitBarcode(QR_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_SERVICE_UNAVAILABLE)));
    }

    @Test
    public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturned_JPG() throws ImagePayloadException, IOException, InterruptedException, ImageCompressException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        createAndValidateImage(VALID_JSON, true, imagePayload);
        when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenReturn(ExpectedFitnoteFormat.Status.SUCCESS);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        verify(validator).validateAndTranslateSubmission(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
        long startTime = System.currentTimeMillis();

        response = resourceUnderTest.checkFitnote(sessionIdParameter);
        while (((System.currentTimeMillis() - startTime) < EXPIRY_TIME_MILLISECONDS) && (!decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("SUCCEEDED"))) {
            assertThat(response.getStatus(), is(equalTo(SC_OK)));

            Thread.sleep(1000);
            response = resourceUnderTest.checkFitnote(sessionIdParameter);
        }

        assertThat(decodeResponse(response.getEntity().toString()).getFitnoteStatus(), is("SUCCEEDED"));
        verify(imageCompressor, times(2)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
        assertThat("should be final compressed image size", getPayloadImageSize(), is(equalTo(controllerConfiguration.getTargetImageSizeKB() * 1000)));
    }

    @Test
    public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturned_PDF() throws ImagePayloadException, IOException, InterruptedException, ImageCompressException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        when(controllerConfiguration.getPdfScanDPI()).thenReturn(300);

        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(PDF_FITNOTE_IMAGE);
        createAndValidateImage(PDF_JSON, true, imagePayload);

        when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenReturn(ExpectedFitnoteFormat.Status.SUCCESS);
        Response response = resourceUnderTest.submitFitnote(PDF_JSON);
        verify(validator).validateAndTranslateSubmission(PDF_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
        long startTime = System.currentTimeMillis();

        response = resourceUnderTest.checkFitnote(sessionIdParameter);
        while (((System.currentTimeMillis() - startTime) < EXPIRY_TIME_MILLISECONDS) && (!decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("SUCCEEDED"))) {
            assertThat(response.getStatus(), is(equalTo(SC_OK)));

            Thread.sleep(1000);
            response = resourceUnderTest.checkFitnote(sessionIdParameter);
        }

        assertThat(decodeResponse(response.getEntity().toString()).getFitnoteStatus(), is("SUCCEEDED"));
        verify(imageCompressor, times(2)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
        assertThat("should be final compressed image size", getPayloadImageSize(), is(equalTo(controllerConfiguration.getTargetImageSizeKB() * 1000)));
    }

    @Test
    public void testThatTimeoutOnQueryCausesAnEror() throws ImagePayloadException, IOException, InterruptedException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        createAndValidateImage(VALID_JSON, true, imagePayload);

        when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenReturn(ExpectedFitnoteFormat.Status.SUCCESS);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        verify(validator).validateAndTranslateSubmission(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
        long startTime = System.currentTimeMillis();

        response = resourceUnderTest.checkFitnote(sessionIdParameter);
        while (((System.currentTimeMillis() - startTime) < EXPIRY_TIME_MILLISECONDS) && (!decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("NEVER_GONNA_MATCH"))) {
            assertThat(response.getStatus(), is(equalTo(SC_OK)));

            Thread.sleep(1000);
            response = resourceUnderTest.checkFitnote(sessionIdParameter);
        }

        assertFalse(decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("NEVER_GONNA_MATCH"));
        assertThat("should be final compressed image size", getPayloadImageSize(), is(equalTo(controllerConfiguration.getTargetImageSizeKB() * 1000)));
    }

    @Test
    public void jsonIsPassedIntoServiceWithOcrDisabledAnd202IsReturned() throws ImagePayloadException, IOException, InterruptedException, ImageCompressException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(false);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        createAndValidateImage(VALID_JSON, true, imagePayload);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        verify(validator).validateAndTranslateSubmission(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
        long startTime = System.currentTimeMillis();

        response = resourceUnderTest.checkFitnote(sessionIdParameter);
        while (((System.currentTimeMillis() - startTime) < EXPIRY_TIME_MILLISECONDS) && (!decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("SUCCEEDED"))) {
            assertThat(response.getStatus(), is(equalTo(SC_OK)));

            Thread.sleep(1000);
            response = resourceUnderTest.checkFitnote(sessionIdParameter);
        }

        assertThat(decodeResponse(response.getEntity().toString()).getFitnoteStatus(), is("SUCCEEDED"));
        verify(imageCompressor, times(1)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
        assertThat("should be final compressed image size", getPayloadImageSize(), is(equalTo(controllerConfiguration.getTargetImageSizeKB() * 1000)));
    }

    @Test
    public void invalidJsonReturns400() throws ImagePayloadException {
        String json = "invalidJson";
        when(validator.validateAndTranslateSubmission(json)).thenThrow(new ImagePayloadException("Thrown for test purposes"));
        Response response = resourceUnderTest.submitFitnote(json);
        assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
        verifyNoMoreInteractions(ocrChecker);
    }

    @Test
    public void exceptionWhileTryingOCRIsTranslatedInto500() throws ImagePayloadException, IOException, InterruptedException {
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        String json = "json";
        when(validator.validateAndTranslateSubmission(json)).thenReturn(imagePayload);
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenThrow(new IOException("thrown for test purposes"));
        Response response = resourceUnderTest.submitFitnote(json);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
        long startTime = System.currentTimeMillis();

        response = resourceUnderTest.checkFitnote(sessionIdParameter);
        while (((System.currentTimeMillis() - startTime) < EXPIRY_TIME_MILLISECONDS) && (!decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("FAILED_ERROR"))) {
            assertThat(response.getStatus(), is(equalTo(SC_OK)));

            Thread.sleep(1000);
            response = resourceUnderTest.checkFitnote(sessionIdParameter);
        }

        assertThat(decodeResponse(response.getEntity().toString()).getFitnoteStatus(), is("FAILED_ERROR"));
    }

    @Test
    public void confirmWhenImagePassesOCRCheck200AndSessionIdIsReturned() throws ImagePayloadException, IOException {
        ImagePayload imagePayload = new ImagePayload();
        String base64Image = "Base64Image";
        imagePayload.setImage(base64Image);
        String sessionId = SESSION;
        imagePayload.setSessionId(sessionId);
        createAndValidateImage(VALID_JSON, true, imagePayload);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        assertThat(response.getStatus(), is(SC_ACCEPTED));
        String entity = (String) response.getEntity();
        assertThat(entity, is(equalTo("{\"sessionId\":\"" + sessionId + "\"}")));
    }

    @Test
    public void confirmWhenImagePassesOCRCheckButFailsCompression200IsReturned() throws ImagePayloadException, IOException, ImageCompressException, InterruptedException {
        when(imageCompressor.compressBufferedImage(any(BufferedImage.class), any(int.class), eq(true))).thenReturn(null);
        ImagePayload imagePayload = new ImagePayload();
        String base64Image = "Base64Image";
        imagePayload.setImage(base64Image);
        String sessionId = SESSION;
        imagePayload.setSessionId(sessionId);
        createAndValidateImage(VALID_JSON, true, imagePayload);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        assertThat(response.getStatus(), is(SC_ACCEPTED));
        long startTime = System.currentTimeMillis();

        response = resourceUnderTest.checkFitnote(sessionIdParameter);
        while (((System.currentTimeMillis() - startTime) < EXPIRY_TIME_MILLISECONDS) && (!decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("FAILED_ERROR"))) {
            assertThat(response.getStatus(), is(equalTo(SC_OK)));

            Thread.sleep(1000);
            response = resourceUnderTest.checkFitnote(sessionIdParameter);
        }

        assertThat(decodeResponse(response.getEntity().toString()).getFitnoteStatus(), is("FAILED_ERROR"));
    }

    @Test
    public void confirmWhenImageFailsOCRCheck400IsReturned() throws ImagePayloadException, IOException, InterruptedException, ImageCompressException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        createAndValidateImage(VALID_JSON, false, imagePayload);

        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
        long startTime = System.currentTimeMillis();

        response = resourceUnderTest.checkFitnote(sessionIdParameter);
        while (((System.currentTimeMillis() - startTime) < EXPIRY_TIME_MILLISECONDS) && (!decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("FAILED_IMG_OCR"))) {
            assertThat(response.getStatus(), is(equalTo(SC_OK)));

            Thread.sleep(1000);
            response = resourceUnderTest.checkFitnote(sessionIdParameter);
        }

        verify(imageCompressor, times(1)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
        assertThat(decodeResponse(response.getEntity().toString()).getFitnoteStatus(), is("FAILED_IMG_OCR"));
    }

    @Test
    public void imageIsStoredInMemoryForLaterRetrieval() throws IOException, ImagePayloadException {
        ImagePayload imagePayload = new ImagePayload();
        String base64Image = "Base64Image";
        imagePayload.setBarcodeImage(base64Image);
        imagePayload.setSessionId(SESSION);
        when(validator.validateAndTranslateBarcodeSubmission(QR_JSON)).thenReturn(imagePayload);
        resourceUnderTest.submitBarcode(QR_JSON);
        assertThat(imageStorage.getPayload(SESSION), is(notNullValue()));
    }

    @Test
    public void failedImagePersistCausesInternalServiceException() throws IOException, ImagePayloadException, InterruptedException {
        String expectedImage = "image";
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(expectedImage);

        when(validator.validateAndTranslateSubmission(VALID_JSON)).thenReturn(imagePayload);
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);

        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));
        long startTime = System.currentTimeMillis();

        response = resourceUnderTest.checkFitnote(sessionIdParameter);
        while (((System.currentTimeMillis() - startTime) < EXPIRY_TIME_MILLISECONDS) && (!decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals("FAILED_ERROR"))) {
            assertThat(response.getStatus(), is(equalTo(SC_OK)));

            Thread.sleep(1000);
            response = resourceUnderTest.checkFitnote(sessionIdParameter);
        }

        assertThat(decodeResponse(response.getEntity().toString()).getFitnoteStatus(), is("FAILED_ERROR"));
    }

    @Test
    public void qrSubmissionReceivesValidJsonAnd202IsReturned() throws ImagePayloadException, IOException {
        ImagePayload payload = new ImagePayload();
        payload.setBarcodeImage(FITNOTE_QR_TEST);
        payload.setSessionId(SESSION);

        payload.setExpiryTime(10L);
        when(validator.validateAndTranslateBarcodeSubmission(FITNOTE_QR_TEST)).thenReturn(payload);
        Response response = resourceUnderTest.submitBarcode(FITNOTE_QR_TEST);
        assertThat(response.getStatus(), is(202));
    }

    @Test
    public void qrSubmissionReceivesInvalidJsonAnd400IsReturned() throws ImagePayloadException {
        String invalidJson = "ThisIsSomeInvalidJson";
        when(validator.validateAndTranslateBarcodeSubmission(invalidJson)).thenThrow(new ImagePayloadException("Thrown for test purposes"));
        Response response = resourceUnderTest.submitBarcode(invalidJson);
        assertThat(response.getStatus(), is(400));
        verifyNoMoreInteractions(ocrChecker);
    }

    private void createAndValidateImage(String json, boolean isValid, ImagePayload imagePayload) throws ImagePayloadException, IOException {
        when(validator.validateAndTranslateSubmission(json)).thenReturn(imagePayload);
        when(ocrChecker.imageContainsReadableText(imagePayload)).thenReturn(isValid ? ExpectedFitnoteFormat.Status.SUCCESS : ExpectedFitnoteFormat.Status.FAILED);
    }

    private StatusItem decodeResponse(String response) throws IOException {
        return new ObjectMapper().readValue(response, StatusItem.class);
    }

    private int getPayloadImageSize() throws ImagePayloadException, IOException {
        return Base64.decode(imageStorage.getPayload(SESSION).getImage()).length;
    }
}