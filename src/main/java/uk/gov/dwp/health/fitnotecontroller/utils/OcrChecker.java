package uk.gov.dwp.health.fitnotecontroller.utils;

import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
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
import java.util.concurrent.TimeUnit;

import static org.bytedeco.javacpp.lept.pixDestroy;

public class OcrChecker {
    private static final String TESSERACT_FOLDER_ERROR = "the tessdata configuration file could not be found in %s";
    private static final Logger LOG = LoggerFactory.getLogger(OcrChecker.class.getName());
    private final FitnoteControllerConfiguration configuration;

    public OcrChecker(FitnoteControllerConfiguration config) {
        this.configuration = config;
    }

    public ExpectedFitnoteFormat.Status imageContainsReadableText(ImagePayload imagePayload) throws IOException {
        byte[] decode = Base64.decodeBase64(imagePayload.getImage());
        String sessionID = imagePayload.getSessionId();
        long startTime = System.currentTimeMillis();
        ExpectedFitnoteFormat.Status imageStatus;

        BufferedImage read;

        ExpectedFitnoteFormat readableImageFormat;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decode)) {
            read = ImageIO.read(inputStream);
        }

        LOG.debug("Image Base64 decoded from string");
        LOG.info("Start OCR checks :: SID: {}", sessionID);

        readableImageFormat = tryImageWithRotations(read, sessionID);
        if (readableImageFormat == null) {
            imageStatus = ExpectedFitnoteFormat.Status.FAILED;

        } else if (readableImageFormat.getFinalImage() != null) {
            String readableImageString;

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(readableImageFormat.getFinalImage(), "jpg", outputStream);
                readableImageString = Base64.encodeBase64String(outputStream.toByteArray());
            }

            imagePayload.setImage(readableImageString);
            imageStatus = readableImageFormat.getStatus();

        } else {
            imageStatus = readableImageFormat.getStatus();
        }

        LOG.info("End OCR checks :: SID: {} {} :: Response time (seconds) = {}", sessionID, imageStatus, ((System.currentTimeMillis() - startTime) / 1000));
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

                LOG.debug("Completed thread {} (priority {}) for sessionId {} with rotation {}", Thread.currentThread().getName(), Thread.currentThread().getPriority(), sessionID, rotation);
                return fitnoteFormat;
            }
        };
    }

    private synchronized ExpectedFitnoteFormat tryImageWithRotations(BufferedImage originalImage, String sessionID) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        int[] rotationAngles = {0, 180, 90, 270};

        CompletionService<ExpectedFitnoteFormat> threadStack = new ExecutorCompletionService<>(executorService);
        try {
            int threadPriority = Thread.MAX_PRIORITY;

            for (int angle : rotationAngles) {
                threadStack.submit(buildCallable(originalImage, sessionID, angle, threadPriority));
                threadPriority = threadPriority - 3;
            }

            for (int stack = 0; stack < rotationAngles.length; stack++) {
                ExpectedFitnoteFormat result = threadStack.take().get();

                if ((result != null) && ((result.getFinalImage() != null) || (result.getStatus().equals(ExpectedFitnoteFormat.Status.PARTIAL)))) {
                    return result;
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Thread error :: {}", e.getMessage());
            LOG.debug(e.getClass().getName(), e);

            if (e.getCause() instanceof IOException) {
                throw new IOException(e);
            }

        } finally {
            long startTime = System.currentTimeMillis();
            try {
                executorService.shutdownNow();
                executorService.awaitTermination(1, TimeUnit.SECONDS);

            } catch (InterruptedException e) {
                LOG.error("{} :: {}", e.getClass().getName(), e.getMessage());
                LOG.debug(e.getClass().getName(), e);
                Thread.currentThread().interrupt();
            }

            LOG.info("Threads closed from OCR in {} ms", System.currentTimeMillis() - startTime);
        }

        LOG.debug("SID: {} Image Failed OCR", sessionID);
        return null;
    }

    private void logResult(ExpectedFitnoteFormat localFitnoteFormat, int rotation, String sessionID) {
        int maxChars = configuration.getMaxLogChars();
        String tlChars = localFitnoteFormat.getTopLeftStringToLog().length() > maxChars ? localFitnoteFormat.getTopLeftStringToLog().substring(0, maxChars) : localFitnoteFormat.getTopLeftStringToLog();
        String trChars = localFitnoteFormat.getTopRightStringToLog().length() > maxChars ? localFitnoteFormat.getTopRightStringToLog().substring(0, maxChars) : localFitnoteFormat.getTopRightStringToLog();
        String blChars = localFitnoteFormat.getBaseLeftStringToLog().length() > maxChars ? localFitnoteFormat.getBaseLeftStringToLog().substring(0, maxChars) : localFitnoteFormat.getBaseLeftStringToLog();
        String brChars = localFitnoteFormat.getBaseRightStringToLog().length() > maxChars ? localFitnoteFormat.getBaseRightStringToLog().substring(0, maxChars) : localFitnoteFormat.getBaseRightStringToLog();
        LOG.info("Running OCR checks :: SID:{} {} @ {}°", sessionID, localFitnoteFormat.getLoggingString(), rotation);
        LOG.debug("**** Top Left String   :: {}", tlChars);
        LOG.debug("**** Top Right String  :: {}", trChars);
        LOG.debug("**** Base Left String  :: {}", blChars);
        LOG.debug("**** Base Right String :: {}", brChars);
    }

    private void ocrScanFitnote(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int rotation) throws IOException {
        LOG.debug("OCR :: Brightness target {}, Contrast {}", configuration.getTargetBrightness(), configuration.getContrastCutOff());
        int height = fitnoteFormat.getFinalImage().getHeight();
        int width = fitnoteFormat.getFinalImage().getWidth();

        ocrScanTopLeft(ocr, fitnoteFormat, width, height, configuration.getHighTarget(), rotation, configuration.getOcrVerticalSlice());

        if (fitnoteFormat.getTopLeftPercentage() < configuration.getDiagonalTarget()) {
            LOG.info("TL {} < {}, impossible diagonal match, move to BL", fitnoteFormat.getTopLeftPercentage(), configuration.getDiagonalTarget());

        } else {

            ocrScanBaseRight(ocr, fitnoteFormat, width, height, fitnoteFormat.getTopLeftPercentage() >= configuration.getHighTarget() ? configuration.getDiagonalTarget() : configuration.getHighTarget(), rotation, configuration.getOcrVerticalSlice());

            if (fitnoteFormat.validateFitnotePassed().equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
                LOG.info("no need to continue scanning, matched on TL/BR");
                return;
            }
        }

        ocrScanBaseLeft(ocr, fitnoteFormat, width, height, configuration.getHighTarget(), rotation, configuration.getOcrVerticalSlice());

        if (fitnoteFormat.validateFitnotePassed().equals(ExpectedFitnoteFormat.Status.SUCCESS)) {
            LOG.info("no need to continue scanning, matched on LHS");
            return;
        }
        if (fitnoteFormat.getBaseLeftPercentage() < configuration.getDiagonalTarget()) {
            LOG.info("no checking of TR, BL < minimum percentage");
            return;
        }

        ocrScanTopRight(ocr, fitnoteFormat, width, height, configuration.getHighTarget(), rotation, configuration.getOcrVerticalSlice());
    }

    private void ocrScanTopLeft(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int width, int height, int targetPercentage, int rotation, int verticalSlice) throws IOException {
        BufferedImage subImage = fitnoteFormat.getFinalImage().getSubimage(0, 0, width / 2, height / verticalSlice);
        ocrApplyImageFilters(subImage, ocr, fitnoteFormat, "TL", targetPercentage, rotation);
    }

    private void ocrScanTopRight(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int width, int height, int targetPercentage, int rotation, int verticalSlice) throws IOException {
        BufferedImage subImage = fitnoteFormat.getFinalImage().getSubimage(width / 2, 0, width / 2, height / verticalSlice);
        ocrApplyImageFilters(subImage, ocr, fitnoteFormat, "TR", targetPercentage, rotation);
    }

    private void ocrScanBaseLeft(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int width, int height, int targetPercentage, int rotation, int verticalSlice) throws IOException {
        int heightDifferential = height / verticalSlice;

        BufferedImage subImage = fitnoteFormat.getFinalImage().getSubimage(0, heightDifferential * (verticalSlice - 1), width / 2, heightDifferential);
        ocrApplyImageFilters(subImage, ocr, fitnoteFormat, "BL", targetPercentage, rotation);
    }

    private void ocrScanBaseRight(tesseract.TessBaseAPI ocr, ExpectedFitnoteFormat fitnoteFormat, int width, int height, int targetPercentage, int rotation, int verticalSlice) throws IOException {
        int heightDifferential = height / verticalSlice;

        BufferedImage subImage = fitnoteFormat.getFinalImage().getSubimage(width / 2, heightDifferential * (verticalSlice - 1), width / 2, heightDifferential);
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
        int highPercentage = 0;
        String filter = "";

        LOG.info("*** START {} CHECKS, AIMING FOR {} PERCENTAGE @ {} ROTATION ***", location, targetPercentage, rotation);
        while ((highPercentage < targetPercentage) && (filterApplications < 4)) {
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
                    LOG.info("No filter available for {}", filterApplications);
                    break;
            }

            if (workingImage != null) {
                LOG.info("OCR checking using filter '{}' on {} page location at {} rotation", filter, location, rotation);

                if ("TL".equalsIgnoreCase(location)) {
                    fitnoteFormat.scanTopLeft(ocrScanSubImage(workingImage, ocr));
                    highPercentage = fitnoteFormat.getTopLeftPercentage();

                } else if ("TR".equalsIgnoreCase(location)) {
                    fitnoteFormat.scanTopRight(ocrScanSubImage(workingImage, ocr));
                    highPercentage = fitnoteFormat.getTopRightPercentage();

                } else if ("BL".equalsIgnoreCase(location)) {
                    fitnoteFormat.scanBaseLeft(ocrScanSubImage(workingImage, ocr));
                    highPercentage = fitnoteFormat.getBaseLeftPercentage();

                } else {
                    fitnoteFormat.scanBaseRight(ocrScanSubImage(workingImage, ocr));
                    highPercentage = fitnoteFormat.getBaseRightPercentage();
                }
            }

            if ((filterApplications == 1) && (highPercentage < configuration.getDiagonalTarget())) {
                LOG.info("Abandoned time-costly checks after 2 filters with < {} percentage OCR for location {} at rotation {}", configuration.getDiagonalTarget(), location, rotation);
                return;
            }

            filterApplications++;
        }

        LOG.info("*** END {} CHECKS ***", location);
    }
}

