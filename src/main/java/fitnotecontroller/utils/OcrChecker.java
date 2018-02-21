package fitnotecontroller.utils;

import fitnotecontroller.application.FitnoteControllerConfiguration;
import fitnotecontroller.domain.ExpectedFitnoteFormat;
import fitnotecontroller.domain.ImagePayload;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.tesseract;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.bytedeco.javacpp.lept.pixDestroy;

public class OcrChecker {
    private static final String TESSERACT_FOLDER_ERROR = "the tessdata configuration file could not be found in %s";
    private static final Logger LOG = DwpEncodedLogger.getLogger(OcrChecker.class.getName());
    private final FitnoteControllerConfiguration configuration;

    public OcrChecker(FitnoteControllerConfiguration config) {
        this.configuration = config;
    }

    public ExpectedFitnoteFormat.Status imageContainsReadableText(ImagePayload imagePayload) throws IOException {
        byte[] decode = Base64.decodeBase64(imagePayload.getImage());
        String sessionID = imagePayload.getSessionId();
        ExpectedFitnoteFormat.Status imageStatus;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(decode);
        BufferedImage read = ImageIO.read(inputStream);
        LOG.debug("Image Base64 decoded from string");
        LOG.info(String.format("Start OCR checks :: SID:%s", sessionID));

        ExpectedFitnoteFormat readableImageFormat = tryImageWithRotations(read, sessionID);
        if (readableImageFormat == null) {
            imageStatus = ExpectedFitnoteFormat.Status.FAILED;

        } else if (readableImageFormat.getFinalImage() != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(readableImageFormat.getFinalImage(), "jpg", outputStream);
            String readableImageString = Base64.encodeBase64String(outputStream.toByteArray());
            imagePayload.setImage(readableImageString);
            imageStatus = readableImageFormat.getStatus();

        } else {
            imageStatus = readableImageFormat.getStatus();
        }

        LOG.info(String.format("End OCR checks :: SID:%s %s", sessionID, imageStatus));
        return imageStatus;
    }

    private Callable<ExpectedFitnoteFormat> buildCallable(BufferedImage originalImage, String sessionID, int rotation, int threadPriority) {
        return () -> {
            try (tesseract.TessBaseAPI instance = new tesseract.TessBaseAPI()) {
                Thread.currentThread().setPriority(threadPriority);

                if (instance.Init(configuration.getTesseractFolderPath(), "eng") != 0) {
                    throw new IOException(String.format(TESSERACT_FOLDER_ERROR, configuration.getTesseractFolderPath()));
                }

                instance.SetVariable("debug_file", "/dev/null");

                ExpectedFitnoteFormat fitnoteFormat = new ExpectedFitnoteFormat(configuration);

                if (rotation > 0) {
                    fitnoteFormat.setFinalImage(ImageUtils.createRotatedCopy(originalImage, rotation));
                } else {
                    fitnoteFormat.setFinalImage(originalImage);
                }

                ocrScanFitnote(instance, fitnoteFormat, rotation);
                logResult(fitnoteFormat, rotation, sessionID);

                if (!fitnoteFormat.getStatus().equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
                    fitnoteFormat.setFinalImage(null);
                }

                LOG.debug(String.format("Completed thread %s (priority %d) for sessionId %s with rotation %d", Thread.currentThread().getName(), Thread.currentThread().getPriority(), sessionID, rotation));
                return fitnoteFormat;
            }
        };
    }

    private synchronized ExpectedFitnoteFormat tryImageWithRotations(BufferedImage originalImage, String sessionID) throws IOException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        int[] rotationAngles = {0, 180, 90, 270};

        CompletionService<ExpectedFitnoteFormat> threadStack = new ExecutorCompletionService<>(executorService);
        try {
            int threadPriority = Thread.MAX_PRIORITY;

            for (int angle : rotationAngles) {
                threadStack.submit(buildCallable(originalImage, sessionID, angle, threadPriority));
                threadPriority = threadPriority -3;
            }

            for (int stack = 0; stack < rotationAngles.length; stack++) {
                ExpectedFitnoteFormat result = threadStack.take().get();

                if ((result != null) && ((result.getFinalImage() != null) || (result.getStatus().equals(ExpectedFitnoteFormat.Status.PARTIAL)))) {
                    return result;
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            LOG.error(String.format("Thread error :: %s", e.getMessage()));
            LOG.debug(e);

            if (e.getCause() instanceof IOException) {
                throw new IOException(e);
            }

        } finally {
            executorService.shutdownNow();
        }

        LOG.debug(String.format("SID:%s Image Failed OCR", sessionID));
        return null;
    }

    private void logResult(ExpectedFitnoteFormat localFitnoteFormat, int rotation, String sessionID) {
        int maxChars = configuration.getMaxLogChars();
        String tlChars = localFitnoteFormat.getTopLeftStringToLog().length() > maxChars ? localFitnoteFormat.getTopLeftStringToLog().substring(0, maxChars) : localFitnoteFormat.getTopLeftStringToLog();
        String trChars = localFitnoteFormat.getTopRightStringToLog().length() > maxChars ? localFitnoteFormat.getTopRightStringToLog().substring(0, maxChars) : localFitnoteFormat.getTopRightStringToLog();
        String blChars = localFitnoteFormat.getBaseLeftStringToLog().length() > maxChars ? localFitnoteFormat.getBaseLeftStringToLog().substring(0, maxChars) : localFitnoteFormat.getBaseLeftStringToLog();
        String brChars = localFitnoteFormat.getBaseRightStringToLog().length() > maxChars ? localFitnoteFormat.getBaseRightStringToLog().substring(0, maxChars) : localFitnoteFormat.getBaseRightStringToLog();
        LOG.info(String.format("Running OCR checks :: SID:%s %s @ %d°", null == sessionID ? "null" : sessionID, localFitnoteFormat.getLoggingString(), rotation));
        LOG.debug(String.format("**** Top Left String   :: %s", tlChars));
        LOG.debug(String.format("**** Top Right String  :: %s", trChars));
        LOG.debug(String.format("**** Base Left String  :: %s", blChars));
        LOG.debug(String.format("**** Base Right String :: %s", brChars));
    }

    private void ocrScanFitnote(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int rotation) throws IOException {
        LOG.debug(String.format("OCR :: Brightness target %d, Contrast %d", configuration.getTargetBrightness(), configuration.getContrastCutOff()));
        int height = fitnoteFormat.getFinalImage().getHeight();
        int width = fitnoteFormat.getFinalImage().getWidth();

        ocrScanTopLeft(ocr, fitnoteFormat, width, height, configuration.getHighTarget(), rotation);

        if (fitnoteFormat.getTopLeftPercentage() < configuration.getDiagonalTarget()) {
            LOG.info(String.format("TL %d < %d, impossible diagnonal match, move to BL", fitnoteFormat.getTopLeftPercentage(), configuration.getDiagonalTarget()));

        } else {

            ocrScanBaseRight(ocr, fitnoteFormat, width, height, fitnoteFormat.getTopLeftPercentage() >= configuration.getHighTarget() ? configuration.getDiagonalTarget() : configuration.getHighTarget(), rotation);

            if (fitnoteFormat.validateFitnotePassed().equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
                LOG.info("no need to continue scanning, matched on TL/BR");
                return;
            }
        }

        ocrScanBaseLeft(ocr, fitnoteFormat, width, height, configuration.getHighTarget(), rotation);

        if (fitnoteFormat.validateFitnotePassed().equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
            LOG.info("no need to continue scanning, matched on LHS");
            return;
        }
        if (fitnoteFormat.getBaseLeftPercentage() < configuration.getDiagonalTarget()) {
            LOG.info("no checking of TR, BL < minimum percentage");
            return;
        }

        ocrScanTopRight(ocr, fitnoteFormat, width, height, configuration.getHighTarget(), rotation);
    }

    private void ocrScanTopLeft(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int width, int height, int targetPercentage, int rotation) throws IOException {
        BufferedImage subImage = fitnoteFormat.getFinalImage().getSubimage(0, 0, width / 2, height / 6);
        ocrApplyImageFilters(subImage, ocr, fitnoteFormat, "TL", targetPercentage, rotation);
    }

    private void ocrScanTopRight(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int width, int height, int targetPercentage, int rotation) throws IOException {
        BufferedImage subImage = fitnoteFormat.getFinalImage().getSubimage(width / 2, 0, width / 2, height / 6);
        ocrApplyImageFilters(subImage, ocr, fitnoteFormat, "TR", targetPercentage, rotation);
    }

    private void ocrScanBaseLeft(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int width, int height, int targetPercentage, int rotation) throws IOException {
        int baseDifferential = 6;
        int heightDifferential = height / baseDifferential;

        BufferedImage subImage = fitnoteFormat.getFinalImage().getSubimage(0, heightDifferential * (baseDifferential - 1), width / 2, heightDifferential);
        ocrApplyImageFilters(subImage, ocr, fitnoteFormat, "BL", targetPercentage, rotation);
    }

    private void ocrScanBaseRight(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int width, int height, int targetPercentage, int rotation) throws IOException {
        int baseDifferential = 6;
        int heightDifferential = height / baseDifferential;

        BufferedImage subImage = fitnoteFormat.getFinalImage().getSubimage(width / 2, heightDifferential * (baseDifferential - 1), width / 2, heightDifferential);
        ocrApplyImageFilters(subImage, ocr, fitnoteFormat, "BR", targetPercentage, rotation);
    }

    private String ocrScanSubImage(BufferedImage read, tesseract.TessBaseAPI ocr) throws IOException {
        String returnString = "";
        BytePointer bPointer = null;
        lept.PIX imageObject = null;

        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            ImageIO.write(read, "jpg", outStream);
            ocr.SetPageSegMode(tesseract.PSM_SPARSE_TEXT);

            imageObject = lept.pixReadMem(outStream.toByteArray(), outStream.toByteArray().length);
            ocr.SetImage(imageObject);
            ocr.Recognize(null);
            bPointer = ocr.GetUTF8Text();

            if (null != bPointer) {
                returnString = bPointer.getString().toUpperCase();
            }

            ocr.Clear();

        } finally {
            if (bPointer != null) {
                bPointer.deallocate();
            }
            if (imageObject != null) {
                pixDestroy(imageObject);
            }
        }

        return returnString;
    }

    private void ocrApplyImageFilters(BufferedImage subImage, tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, String location, int targetPercentage, int rotation) throws IOException {
        BufferedImage workingImage = null;
        int filterApplications = 0;
        int higPercentage = 0;
        String filter = "";

        LOG.info(String.format("*** START %s CHECKS, AIMING FOR %d PERCENTAGE @ %d ROTATION ***", location, targetPercentage, rotation));
        while ((higPercentage < targetPercentage) && (filterApplications < 4)) {
            switch (filterApplications) {
                case 0:
                    workingImage = ImageUtils.normaliseBrightness(subImage, configuration.getTargetBrightness(), configuration.getBorderLossPercentage());
                    filter = "brightness";
                    break;
                case 1:
                    workingImage = subImage;
                    filter = "plain";
                    break;
                case 2:
                    workingImage = ImageUtils.increaseContrast(subImage, configuration.getContrastCutOff());
                    filter = "contrast";
                    break;
                case 3:
                    workingImage = ImageUtils.increaseContrast(ImageUtils.formatGrayScale(subImage), configuration.getContrastCutOff());
                    filter = "gr contrast";
                    break;
                default:
                    LOG.info(String.format("No filter available for %d", filterApplications));
                    break;
            }

            if (workingImage != null) {
                LOG.info(String.format("OCR checking using filter '%s' on %s page location at %d rotation", filter, location, rotation));

                if ("TL".equalsIgnoreCase(location)) {
                    fitnoteFormat.scanTopLeft(ocrScanSubImage(workingImage, ocr));
                    higPercentage = fitnoteFormat.getTopLeftPercentage();

                } else if ("TR".equalsIgnoreCase(location)) {
                    fitnoteFormat.scanTopRight(ocrScanSubImage(workingImage, ocr));
                    higPercentage = fitnoteFormat.getTopRightPercentage();

                } else if ("BL".equalsIgnoreCase(location)) {
                    fitnoteFormat.scanBaseLeft(ocrScanSubImage(workingImage, ocr));
                    higPercentage = fitnoteFormat.getBaseLeftPercentage();

                } else {
                    fitnoteFormat.scanBaseRight(ocrScanSubImage(workingImage, ocr));
                    higPercentage = fitnoteFormat.getBaseRightPercentage();
                }
            }

            if ((filterApplications == 1) && (higPercentage <= 0)) {
                LOG.info(String.format("Abandoned time-costly checks after 2 filters with zero percentage OCR for location %s at rotation %d", location, rotation));
                return;
            }

            filterApplications++;
        }

        LOG.info(String.format("*** END %s CHECKS ***", location));
    }
}

