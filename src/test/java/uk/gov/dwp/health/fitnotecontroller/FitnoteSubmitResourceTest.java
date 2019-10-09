package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.googlecode.junittoolbox.PollingWait;
import com.googlecode.junittoolbox.RunnableAssert;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
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
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"squid:S1192", "squid:S3008", "squid:S00116"}) // allow string literals and naming convention for variables (left for clarity)
public class FitnoteSubmitResourceTest {

    private static final String SESSION = "session1";
    private static byte[] COMPRESSED_PAGE_LARGE;
    private static byte[] COMPRESSED_PAGE_FINAL;

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

    @BeforeClass
    public static void init() throws IOException {
        COMPRESSED_PAGE_LARGE = FileUtils.readFileToByteArray(new File("src/test/resources/EmptyPageBigger.jpg"));
        COMPRESSED_PAGE_FINAL = FileUtils.readFileToByteArray(new File("src/test/resources/EmptyPage.jpg"));

        LANDSCAPE_FITNOTE_IMAGE = Base64.encodeFromFile("src/test/resources/FullPage_Landscape.jpg");
        PORTRAIT_FITNOTE_IMAGE = Base64.encodeFromFile("src/test/resources/FullPage_Portrait.jpg");
        PDF_FITNOTE_IMAGE = Base64.encodeFromFile("src/test/resources/FullPage_Portrait.pdf");
        FITNOTE_QR_TEST = Base64.encodeFromFile("src/test/resources/working_barcode.jpg");

        PORTRAIT_JSON = "{\"image\":\"" + PORTRAIT_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
        VALID_JSON = "{\"image\":\"" + LANDSCAPE_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
        QR_JSON = "{\"barcodeImage\":\"" + FITNOTE_QR_TEST + "\",\"sessionId\":\"" + SESSION + "\"}";
        PDF_JSON = "{\"image\":\"" + PDF_FITNOTE_IMAGE + "\",\"sessionId\":\"" + SESSION + "\"}";
    }

    @Before
    public void setup() throws IOException, ImagePayloadException, ImageCompressException, CryptoException {
        when(imageCompressor.compressBufferedImage(any(BufferedImage.class), eq(3), eq(false))).thenReturn(COMPRESSED_PAGE_LARGE);
        when(imageCompressor.compressBufferedImage(any(BufferedImage.class), eq(2), eq(true))).thenReturn(COMPRESSED_PAGE_FINAL);
        when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(3);
        when(controllerConfiguration.getScanTargetImageSizeKb()).thenReturn(3);
        when(controllerConfiguration.getTargetImageSizeKB()).thenReturn(2);
        when(controllerConfiguration.isGreyScale()).thenReturn(true);

        resourceUnderTest = new FitnoteSubmitResource(controllerConfiguration, validator, ocrChecker, imageStorage, imageCompressor);

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
    public void extendSessionCallIsOkWithUnknownSessionIdParameterIsPassedIn() throws ImagePayloadException, IOException, CryptoException {
        when(sessionIdParameter.get()).thenReturn("Unknown session id");
        Response response = resourceUnderTest.extendSession(sessionIdParameter);
        assertThat(response.getStatus(), is(equalTo(SC_OK)));
    }

    @Test
    public void extendSessionCallFailsWhenNoSessionIdParameterIsPassedIn() throws ImagePayloadException, IOException, CryptoException {
        when(sessionIdParameter.isPresent()).thenReturn(false);
        Response response = resourceUnderTest.extendSession(sessionIdParameter);
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
    public void failsWhenThereIsNotEnoughMemoryForPhoto() {
        when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(OVER_MAX_MEMORY);

        Response response = resourceUnderTest.submitFitnote(PORTRAIT_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_SERVICE_UNAVAILABLE)));
    }

    @Test
    public void failsWhenThereIsNotEnoughMemoryForBarcode() {
        when(controllerConfiguration.getEstimatedRequestMemoryMb()).thenReturn(OVER_MAX_MEMORY);

        Response response = resourceUnderTest.submitBarcode(QR_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_SERVICE_UNAVAILABLE)));
    }

    @Test
    public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedJpg() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
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
    }

    @Test
    public void jsonIsPassedIntoServiceWithOcrEnabledAnd202IsReturnedPdf() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
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
    }

    @Test
    public void jsonIsPassedIntoServiceWithOcrDisabledAnd202IsReturned() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(false);
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(LANDSCAPE_FITNOTE_IMAGE);
        createAndValidateImage(VALID_JSON, true, imagePayload);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        verify(validator).validateAndTranslateSubmission(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        examineImagestatusResponseForValueOrTimeout("SUCCEEDED");

        verify(imageCompressor, times(1)).compressBufferedImage(any(BufferedImage.class), anyInt(), anyBoolean());
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
        imagePayload.setImage(Base64.encodeBytes(COMPRESSED_PAGE_LARGE));
        imagePayload.setSessionId(SESSION);
        createAndValidateImage(VALID_JSON, true, imagePayload);
        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        assertThat(response.getStatus(), is(SC_ACCEPTED));

        examineImagestatusResponseForValueOrTimeout("FAILED_ERROR");
    }

    @Test
    public void confirmWhenImageFailsOCRCheck400IsReturned() throws ImagePayloadException, IOException, CryptoException, ImageCompressException, InterruptedException {
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
        ImagePayload imagePayload = imageStorage.getPayload(SESSION);
        imagePayload.setImage(Base64.encodeBytes(COMPRESSED_PAGE_LARGE));

        when(ocrChecker.imageContainsReadableText(any(ImagePayload.class))).thenThrow(new IOException("hello"));
        when(validator.validateAndTranslateSubmission(VALID_JSON)).thenReturn(imagePayload);
        when(controllerConfiguration.isOcrChecksEnabled()).thenReturn(true);

        Response response = resourceUnderTest.submitFitnote(VALID_JSON);
        assertThat(response.getStatus(), is(equalTo(SC_ACCEPTED)));

        examineImagestatusResponseForValueOrTimeout("FAILED_ERROR");
    }

    @Test
    public void qrSubmissionReceivesValidJsonAnd202IsReturned() throws ImagePayloadException {
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

    private void examineImagestatusResponseForValueOrTimeout(String expectedStatus) throws InterruptedException {
        TimeUnit.SECONDS.sleep(1); // pause before first execution to allow for async processes to begin/end
        PollingWait wait = new PollingWait().timeoutAfter(59, SECONDS).pollEvery(1, SECONDS);

        wait.until(new RunnableAssert("checking /imagestatus for current session") {
            @Override
            public void run() throws Exception {
                Response response = resourceUnderTest.checkFitnote(sessionIdParameter);
                assertTrue(decodeResponse(response.getEntity().toString()).getFitnoteStatus().equals(expectedStatus));
            }
        });
    }

    private void createAndValidateImage(String json, boolean isValid, ImagePayload imagePayload) throws ImagePayloadException, IOException {
        when(validator.validateAndTranslateSubmission(json)).thenReturn(imagePayload);
        when(ocrChecker.imageContainsReadableText(imagePayload)).thenReturn(isValid ? ExpectedFitnoteFormat.Status.SUCCESS : ExpectedFitnoteFormat.Status.FAILED);
    }

    private StatusItem decodeResponse(String response) throws IOException {
        return new ObjectMapper().readValue(response, StatusItem.class);
    }
}