package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.googlecode.junittoolbox.PollingWait;
import com.googlecode.junittoolbox.RunnableAssert;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ExpectedFitnoteFormat;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.StatusItem;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.ImageCompressor;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import uk.gov.dwp.health.fitnotecontroller.utils.MemoryChecker;
import uk.gov.dwp.health.fitnotecontroller.utils.OcrChecker;
import gherkin.deps.net.iharder.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FitnoteSubmitResourceTest {

    private static final String SESSION = "session1";
    private Optional sessionIdParameter = Mockito.mock(Optional.class);
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

    @Mock
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
    public void setup() throws IOException, ImagePayloadException, ImageCompressException, CryptoException {
        when(imageCompressor.compressBufferedImage(any(BufferedImage.class), eq(3), eq(false))).thenReturn(new byte[3000]);
        when(imageCompressor.compressBufferedImage(any(BufferedImage.class), eq(2), eq(true))).thenReturn(new byte[2000]);
        when(controllerConfiguration.getSessionExpiryTimeInSeconds()).thenReturn(180000L);
        when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(3);
        when(controllerConfiguration.getMaxAllowedImageReplay()).thenReturn(10);
        when(controllerConfiguration.getScanTargetImageSizeKb()).thenReturn(3);
        when(controllerConfiguration.getImageHashSalt()).thenReturn("salt");
        when(controllerConfiguration.getTargetImageSizeKB()).thenReturn(2);
        when(controllerConfiguration.isGreyScale()).thenReturn(true);

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

        ImagePayload returnValue = new ImagePayload();
        returnValue.setSessionId(SESSION);

        when(imageStorage.getPayload(anyString())).thenReturn(returnValue);

        long freeMemory = MemoryChecker.returnCurrentAvailableMemoryInMb(Runtime.getRuntime());
        OVER_MAX_MEMORY = (int) freeMemory + 300;
    }

    @Test
    public void checkFitnoteCallFailsWhenNoSessionIdParameterExists() throws ImagePayloadException, IOException, CryptoException {
        Optional missingSessionIdParameter = Mockito.mock(Optional.class);
        when(missingSessionIdParameter.isPresent()).thenReturn(false);
        Response response = resourceUnderTest.checkFitnote(missingSessionIdParameter);
        assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
    }

    @Test
    public void checkFitnoteCallIsOkWithUnknownSessionIdParameterIsPassedIn() throws ImagePayloadException, IOException, CryptoException {
        when(sessionIdParameter.get()).thenReturn("Unknown session id");
        Response response = resourceUnderTest.checkFitnote(sessionIdParameter);
        assertThat(response.getStatus(), is(equalTo(SC_OK)));
    }

    @Test
    public void checkFitnoteCallFailsWhenNoSessionIdParameterIsPassedIn() throws ImagePayloadException, IOException, CryptoException {
        when(sessionIdParameter.isPresent()).thenReturn(false);
        Response response = resourceUnderTest.checkFitnote(sessionIdParameter);
        assertThat(response.getStatus(), is(equalTo(SC_BAD_REQUEST)));
    }

    @Test
    public void portraitImageFailsChecksWhenOn() throws IOException, CryptoException, ImagePayloadException, InterruptedException {
        when(controllerConfiguration.isLandscapeImageEnforced()).thenReturn(true);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(PORTRAIT_FITNOTE_IMAGE);
        imagePayload.setSessionId(SESSION);
        createAndValidateImage(PORTRAIT_JSON, true, imagePayload);
        Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
        verify(validator).validateAndTranslateSubmission(PORTRAIT_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        examineImagestatusResponseForValueOrTimeout("FAILED_IMG_SIZE");
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
    public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturned_JPG() throws ImagePayloadException, IOException, CryptoException, InterruptedException, ImageCompressException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        createAndValidateImage(VALID_JSON, true, imagePayload);
        when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenReturn(ExpectedFitnoteFormat.Status.SUCCESS);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        verify(validator).validateAndTranslateSubmission(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        examineImagestatusResponseForValueOrTimeout("SUCCEEDED");

        verify(imageCompressor, times(2)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
        assertThat("should be final compressed image size", getPayloadImageSize(), is(equalTo(controllerConfiguration.getTargetImageSizeKB() * 1000)));
    }

    @Test
    public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturned_PDF() throws ImagePayloadException, IOException, CryptoException, InterruptedException, ImageCompressException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        when(controllerConfiguration.getPdfScanDPI()).thenReturn(300);

        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(PDF_FITNOTE_IMAGE);
        createAndValidateImage(PDF_JSON, true, imagePayload);

        when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenReturn(ExpectedFitnoteFormat.Status.SUCCESS);
        Response response = resourceUnderTest.submitFitnote(PDF_JSON);
        verify(validator).validateAndTranslateSubmission(PDF_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        examineImagestatusResponseForValueOrTimeout("SUCCEEDED");

        verify(imageCompressor, times(2)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
        assertThat("should be final compressed image size", getPayloadImageSize(), is(equalTo(controllerConfiguration.getTargetImageSizeKB() * 1000)));
    }

    @Test
    public void testThatTimeoutOnQueryCausesAnEror() throws ImagePayloadException, IOException, CryptoException, InterruptedException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        createAndValidateImage(VALID_JSON, true, imagePayload);

        when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenReturn(ExpectedFitnoteFormat.Status.SUCCESS);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        verify(validator).validateAndTranslateSubmission(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        try {
            examineImagestatusResponseForValueOrTimeout("NEVER_GONNA_MATCH");
            fail("should always timeout");

        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("RunnableAssert(checking /imagestatus for current session) did not succeed within 60 seconds"));
        }

        assertThat("should be final compressed image size", getPayloadImageSize(), is(equalTo(controllerConfiguration.getTargetImageSizeKB() * 1000)));
    }

    @Test
    public void jsonIsPassedIntoServiceWithOcrDisabledAnd202IsReturned() throws ImagePayloadException, IOException, CryptoException, InterruptedException, ImageCompressException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(false);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        createAndValidateImage(VALID_JSON, true, imagePayload);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        verify(validator).validateAndTranslateSubmission(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        examineImagestatusResponseForValueOrTimeout("SUCCEEDED");

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
    public void exceptionWhileTryingOCRIsTranslatedInto500() throws ImagePayloadException, IOException, CryptoException, InterruptedException {
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        String json = "json";
        when(validator.validateAndTranslateSubmission(json)).thenReturn(imagePayload);
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenThrow(new IOException("thrown for test purposes"));
        Response response = resourceUnderTest.submitFitnote(json);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        examineImagestatusResponseForValueOrTimeout("FAILED_ERROR");
    }

    @Test
    public void confirmWhenImagePassesOCRCheck200AndSessionIdIsReturned() throws ImagePayloadException, IOException, CryptoException {
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
    public void confirmWhenImagePassesOCRCheckButFailsCompression200IsReturned() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
        when(imageCompressor.compressBufferedImage(any(BufferedImage.class), any(int.class), eq(true))).thenReturn(null);
        ImagePayload imagePayload = new ImagePayload();
        String base64Image = "Base64Image";
        imagePayload.setImage(base64Image);
        String sessionId = SESSION;
        imagePayload.setSessionId(sessionId);
        createAndValidateImage(VALID_JSON, true, imagePayload);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        assertThat(response.getStatus(), is(SC_ACCEPTED));

        examineImagestatusResponseForValueOrTimeout("FAILED_ERROR");
    }

    @Test
    public void confirmWhenImageFailsOCRCheck400IsReturned() throws ImagePayloadException, IOException, CryptoException, InterruptedException, ImageCompressException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        createAndValidateImage(VALID_JSON, false, imagePayload);

        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        examineImagestatusResponseForValueOrTimeout("FAILED_IMG_OCR");

        verify(imageCompressor, times(1)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
    }

    @Test
    public void imageIsStoredInMemoryForLaterRetrieval() throws IOException, CryptoException, ImagePayloadException {
        ImagePayload imagePayload = new ImagePayload();
        String base64Image = "Base64Image";
        imagePayload.setBarcodeImage(base64Image);
        imagePayload.setSessionId(SESSION);
        when(validator.validateAndTranslateBarcodeSubmission(QR_JSON)).thenReturn(imagePayload);
        resourceUnderTest.submitBarcode(QR_JSON);
        assertThat(imageStorage.getPayload(SESSION), is(notNullValue()));
    }

    @Test
    public void failedImagePersistCausesInternalServiceException() throws IOException, CryptoException, ImagePayloadException, InterruptedException {
        String expectedImage = "image";
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(expectedImage);

        when(validator.validateAndTranslateSubmission(VALID_JSON)).thenReturn(imagePayload);
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);

        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        examineImagestatusResponseForValueOrTimeout("FAILED_ERROR");
    }

    @Test
    public void qrSubmissionReceivesValidJsonAnd202IsReturned() throws ImagePayloadException, IOException, CryptoException {
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

    private void examineImagestatusResponseForValueOrTimeout(String expectedStatus) {
        PollingWait wait = new PollingWait().timeoutAfter(60, SECONDS).pollEvery(1, SECONDS);

        wait.until(new RunnableAssert("checking /imagestatus for current session") {
            @Override
            public void run() throws Exception {
                Response response = resourceUnderTest.checkFitnote(sessionIdParameter);
                assertTrue(decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals(expectedStatus));
            }
        });
    }

    private void createAndValidateImage(String json, boolean isValid, ImagePayload imagePayload) throws ImagePayloadException, IOException, CryptoException {
        when(validator.validateAndTranslateSubmission(json)).thenReturn(imagePayload);
        when(ocrChecker.imageContainsReadableText(imagePayload)).thenReturn(isValid ? ExpectedFitnoteFormat.Status.SUCCESS : ExpectedFitnoteFormat.Status.FAILED);
    }

    private StatusItem decodeResponse(String response) throws IOException {
        return new ObjectMapper().readValue(response, StatusItem.class);
    }

    private int getPayloadImageSize() throws ImagePayloadException, IOException, CryptoException {
        return Base64.decode(imageStorage.getPayload(SESSION).getImage()).length;
    }
}