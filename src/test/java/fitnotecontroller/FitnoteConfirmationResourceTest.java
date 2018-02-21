package fitnotecontroller;

import fitnotecontroller.domain.ImagePayload;
import fitnotecontroller.exception.ImagePayloadException;
import fitnotecontroller.utils.JsonValidator;
import org.apache.http.HttpStatus;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class FitnoteConfirmationResourceTest {

    private static final String VALID_JSON = "{\"sessionId\":\"%s\"}";
    @Mock
    private ImageStorage imageStore;
    @Mock
    private JsonValidator jsonValidator;
    private FitnoteConfirmationResource resourceUnderTest;

    @Before
    public void setup() {
        resourceUnderTest = new FitnoteConfirmationResource(imageStore, jsonValidator);
    }

    @Test
    public void sendNinoToServiceRespondsWith200ForKnownSessionId() throws IOException, ImagePayloadException {
        String sessionId = "123456";
        String nino = "AA370773A";
        String json = buildNinoJsonPayload(sessionId, nino);
        ImagePayload newPayload = buildNinoPayload(sessionId, nino);

        ImagePayload originalImagePayload = new ImagePayload();
        originalImagePayload.setSessionId(sessionId);
        when(jsonValidator.validateAndTranslateConfirmation(json)).thenReturn(newPayload);
        when(imageStore.updateNinoDetails(newPayload)).thenReturn(originalImagePayload);

        Response response = resourceUnderTest.confirmFitnote(json);

        assertThat(response.getStatus(), is(equalTo(HttpStatus.SC_OK)));
        String entity = (String) response.getEntity();
        assertThat(entity, Is.is(equalTo(String.format(VALID_JSON, sessionId))));

        verify(imageStore).updateNinoDetails(newPayload);

    }

    @Test
    public void sendMobileToServiceRespondsWith200ForKnownSessionId() throws IOException, ImagePayloadException {
        String sessionId = "123456";
        String mobileNumber = "0113999999";
        String json = buildMobileJsonPayload(sessionId, mobileNumber);
        ImagePayload newPayload = buildMobilePayload(sessionId, mobileNumber);

        ImagePayload originalImagePayload = new ImagePayload();
        originalImagePayload.setSessionId(sessionId);
        when(jsonValidator.validateAndTranslateMobileConfirmation(json)).thenReturn(newPayload);
        when(imageStore.updateMobileDetails(newPayload)).thenReturn(originalImagePayload);

        Response response = resourceUnderTest.confirmMobile(json);

        assertThat(response.getStatus(), is(equalTo(HttpStatus.SC_OK)));
        String entity = (String) response.getEntity();
        assertThat(entity, Is.is(equalTo(String.format(VALID_JSON, sessionId))));

        verify(imageStore).updateMobileDetails(newPayload);

    }

    private ImagePayload buildMobilePayload(String sessionId, String mobileNumber) {
        ImagePayload payload = new ImagePayload();
        payload.setSessionId(sessionId);
        payload.setMobileNumber(mobileNumber);
        return payload;
    }

    private ImagePayload buildNinoPayload(String sessionId, String nino) {
        ImagePayload payload = new ImagePayload();
        payload.setSessionId(sessionId);
        payload.setNino(nino);
        return payload;
    }

    @Test
    public void invalidJsonOnConfirmationReturns400() throws IOException, ImagePayloadException {
        String json = "{\"nino\":\"AA370773A\"," +
                "\"mobileNumber\":\"0113999999\"}";
        when(jsonValidator.validateAndTranslateConfirmation(json)).thenThrow(new ImagePayloadException("thrown for test purposes"));
        Response response = resourceUnderTest.confirmFitnote(json);
        assertThat(response.getStatus(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));
        verifyNoMoreInteractions(imageStore);
    }

    private String buildNinoJsonPayload(String sessionId, String nino) {
        return "{\"sessionId\":\"" + sessionId + "\"," +
                "\"nino\":\"" + nino + "\"}";
    }

    private String buildMobileJsonPayload(String sessionId, String mobileNumber) {
        return "{\"sessionId\":\"" + sessionId + "\"," +
                "\"mobileNumber\":\"" + mobileNumber + "\"}";
    }

}