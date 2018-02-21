package fitnotecontroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import fitnotecontroller.application.FitnoteControllerConfiguration;
import fitnotecontroller.domain.Address;
import fitnotecontroller.domain.Declaration;
import fitnotecontroller.domain.ImagePayload;
import fitnotecontroller.exception.DeclarationException;
import fitnotecontroller.exception.ImagePayloadException;
import fitnotecontroller.utils.JsonValidator;
import gov.dwp.securecomms.drs.DrsCommunicatorException;
import gov.dwp.securecomms.tls.TLSGeneralException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.dwp.health.utilities.rabbitmq.PublishSubscribe;
import uk.gov.dwp.health.utilities.rabbitmq.exceptions.EventsManagerException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FitnoteDeclarationResourceTest {

    private FitnoteDeclarationResource resource;
    private static final String ACCEPTED_DECLARATION = "{ \"sessionId\" : \"123456\" , \"accepted\" : true }";
    private static final String DECLINED_DECLARATION = "{ \"sessionId\" : \"123456\" , \"accepted\" : false}";
    private static final String INVALID_DECLARATION = "{ \"sessionId\" : \"123456\"  }";
    private static final String EMPTY_DECLARATION = "{ \"sessionId\" :  , \"accepted\" : }";
    private static final String VALID_NEW_ADDRESS = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";

    @Mock
    private ImageStorage imageStore;
    @Mock
    private JsonValidator jsonValidator;
    @Mock
    private PublishSubscribe publishSubscribe;
    @Mock
    private FitnoteControllerConfiguration config;

    @Before
    public void setup() throws URISyntaxException {
        resource = new FitnoteDeclarationResource(imageStore, publishSubscribe, config);

        when(config.getRabbitMqURI()).thenReturn(new URI("amqp://system:manager@localhost:15671"));
        when(config.getEasPostCodeLookUpUrl()).thenReturn("http://esa-lookup-service:8086");
        when(config.getRabbitExchangeName()).thenReturn("test.exchange");
        when(config.getEventRoutingKey()).thenReturn("route.one");
    }

    @Test
    public void returns200WithValidAcceptedDeclaration() throws DeclarationException, IOException, ImagePayloadException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am image", "123456", "NINO", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void return400WithMissingImagePayload() throws IOException, DeclarationException, ImagePayloadException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload(null, "123456", "NINO", "123456", false);
        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity().toString(), containsString("without a Fitnote Image"));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void return400WithMissingNinoPayload() throws IOException, DeclarationException, ImagePayloadException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am an image", "123456", null, "123456", true);
        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity().toString(), containsString("without a valid NINO"));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void return400WithMissingAddress() throws IOException, DeclarationException, ImagePayloadException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload("i am an image", "123456", "NINO", "123456", true);

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity().toString(), containsString("Address must be specified"));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void returns400WithInvalidDeclaration() throws DeclarationException, IOException {
        when(jsonValidator.validateAndTranslateDeclaration(INVALID_DECLARATION)).thenThrow(new DeclarationException("test exception"));

        Response response = resource.submitDeclaration(INVALID_DECLARATION);
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void returns400WithEmptyDeclaration() throws DeclarationException, IOException {
        when(jsonValidator.validateAndTranslateDeclaration(EMPTY_DECLARATION)).thenThrow(new DeclarationException("test exception"));

        Response response = resource.submitDeclaration(EMPTY_DECLARATION);
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void returns400WithADeclinedDeclaration() throws DeclarationException, IOException, DrsCommunicatorException, ImagePayloadException {
        ImagePayload payload = buildImagePayload(" ", "123456", "NINO", "123456", true);

        when(jsonValidator.validateAndTranslateDeclaration(DECLINED_DECLARATION)).thenThrow(new DeclarationException("Invalid value"));
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(DECLINED_DECLARATION);
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void returns500WhenAnUnexpectedExceptionIsThrown() throws DeclarationException {
        when(jsonValidator.validateAndTranslateDeclaration("{\"test\":\"string\"}")).thenThrow(DeclarationException.class);
        Response response = resource.submitDeclaration("{\"test\":\"string\"}");

        assertThat(response.getEntity(), is("Unable to process request"));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void returns500WhenARabbitExceptionIsThrown() throws IOException, DeclarationException, ImagePayloadException, UnrecoverableKeyException, NoSuchAlgorithmException, URISyntaxException, TimeoutException, TLSGeneralException, KeyStoreException, CertificateException, KeyManagementException, EventsManagerException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload(" ", "123456", "NINO", "123456", true);

        payload.setClaimantAddress(new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class));

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        doThrow(new EventsManagerException("i am an exception!")).when(publishSubscribe).publishBasicTextMessageToExchange(eq("test.exchange"), eq("route.one"), anyString(), any(AMQP.BasicProperties.class));

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void returns400WhenAcceptedDeclarationWithNoNino() throws DeclarationException, IOException, ImagePayloadException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);

        ImagePayload payload = buildImagePayload(" ", "123456", "", "", true);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity(), is("Cannot process declaration without a valid NINO"));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void returns400WhenAcceptedDeclarationWithBadImageCheck() throws DeclarationException, IOException, ImagePayloadException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);

        ImagePayload payload = buildImagePayload("i am image", "123456", "NINO", "234567", false);

        Address mockNewAddress = new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class);
        payload.setClaimantAddress(mockNewAddress);

        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getEntity(), is("Cannot process declaration without a successfully scanned Fitnote Image"));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void returns200WithValidAddress() throws DeclarationException, IOException, DrsCommunicatorException, ImagePayloadException {
        Declaration declaration = new ObjectMapper().readValue(ACCEPTED_DECLARATION, Declaration.class);
        ImagePayload payload = buildImagePayload(" ", "123456", "NINO", "123456", true);

        Address mockNewAddress = new ObjectMapper().readValue(VALID_NEW_ADDRESS, Address.class);
        payload.setClaimantAddress(mockNewAddress);

        when(jsonValidator.validateAndTranslateDeclaration(ACCEPTED_DECLARATION)).thenReturn(declaration);
        when(imageStore.getPayload("123456")).thenReturn(payload);

        Response response = resource.submitDeclaration(ACCEPTED_DECLARATION);
        assertThat(response.getStatus(), is(200));
    }

    /*
     * private methods
     */

    private ImagePayload buildImagePayload(String imageString, String sessionId, String nino, String mobileNum, boolean imagePassed) {
        ImagePayload payload = new ImagePayload();
        payload.setImage(imageString);
        payload.setSessionId(sessionId);
        payload.setNino(nino);
        payload.setMobileNumber(mobileNum);

        if (imagePassed) {
            payload.setFitnoteCheckStatus(ImagePayload.Status.SUCCEEDED);
        } else {
            payload.setFitnoteCheckStatus(ImagePayload.Status.CREATED);
        }

        return payload;
    }
}
