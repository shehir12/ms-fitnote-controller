package fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.AMQP;
import fitnotecontroller.application.FitnoteControllerConfiguration;
import fitnotecontroller.domain.Declaration;
import fitnotecontroller.domain.FitnoteMetadata;
import fitnotecontroller.domain.ImagePayload;
import fitnotecontroller.exception.DeclarationException;
import fitnotecontroller.exception.ImagePayloadException;
import fitnotecontroller.utils.JsonValidator;
import gov.dwp.securecomms.drs.DrsPayloadBuilder;
import gov.dwp.securecomms.tls.TLSGeneralException;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import uk.gov.dwp.health.utilities.rabbitmq.PublishSubscribe;
import uk.gov.dwp.health.utilities.rabbitmq.exceptions.EventsManagerException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Path("/")
public class FitnoteDeclarationResource {
    private static final Logger LOG = DwpEncodedLogger.getLogger(FitnoteDeclarationResource.class.getName());
    private static final String ERROR_MSG = "Unable to process request";
    private FitnoteControllerConfiguration config;
    private PublishSubscribe rabbitMqPublish;
    private JsonValidator jsonValidator;
    private ImageStorage imageStore;

    public FitnoteDeclarationResource(ImageStorage imageStore,
                                      PublishSubscribe rabbitMqPublish,
                                      FitnoteControllerConfiguration config) {
        this(imageStore, new JsonValidator(), rabbitMqPublish, config);
    }

    public FitnoteDeclarationResource(ImageStorage imageStore,
                                      JsonValidator jsonValidator,
                                      PublishSubscribe rabbitMqPublish,
                                      FitnoteControllerConfiguration config) {
        this.rabbitMqPublish = rabbitMqPublish;
        this.jsonValidator = jsonValidator;
        this.imageStore = imageStore;
        this.config = config;
    }

    @POST
    @Path("/declaration")
    @Produces(MediaType.APPLICATION_JSON)
    public Response submitDeclaration(String jsonBody) {
        try {
            Declaration declaration = jsonValidator.validateAndTranslateDeclaration(jsonBody);
            LOG.debug("Json validated correctly");

            if (!declaration.isAccepted()) {
                LOG.info("Declaration was REJECTED by the user");
                imageStore.clearSession(declaration.getSessionId());
                return Response.status(HttpStatus.SC_OK).build();
            }

            LOG.info("Declaration ACCEPTED by the user");
            ImagePayload imagePayloadToSubmit = imageStore.getPayload(declaration.getSessionId());

            if (imagePayloadToSubmit.getImage() == null) {
                LOG.debug(String.format("REJECT :: no image data to process for session %s", declaration.getSessionId()));
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Cannot process declaration without a Fitnote Image").build();
            }

            if (!imagePayloadToSubmit.getFitnoteCheckStatus().equals(ImagePayload.Status.SUCCEEDED)) {
                LOG.debug(String.format("REJECT :: the fitnote image was not scanned successfully for session %s", declaration.getSessionId()));
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Cannot process declaration without a successfully scanned Fitnote Image").build();
            }

            if ((imagePayloadToSubmit.getNino() == null) || (imagePayloadToSubmit.getNino().isEmpty())) {
                LOG.debug(String.format("REJECT :: no NINO specified for session %s", declaration.getSessionId()));
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Cannot process declaration without a valid NINO").build();
            }

            if (imagePayloadToSubmit.getClaimantAddress() == null) {
                LOG.debug(String.format("REJECT :: Claimant address has not been specified for session %s", declaration.getSessionId()));
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Address must be specified").build();
            }

            String rabbitUrl = String.format("%s://%s:%d", config.getRabbitMqURI().getScheme(), config.getRabbitMqURI().getHost(), config.getRabbitMqURI().getPort());

            LOG.debug("Address on fitnote is supplied");
            LOG.debug(String.format("Session Id %s is ok to process", declaration.getSessionId()));
            LOG.info(String.format("Post image payload directly to RabbitMQ (%s)", rabbitUrl));
            String transactionId = drsDispatchPayload(imagePayloadToSubmit);

            LOG.info(String.format("Clear all data for session %s with correlation id '%s'", declaration.getSessionId(), transactionId));
            imageStore.clearSession(declaration.getSessionId());

            LOG.info(String.format("Successfully posted image data to RabbitMQ (%s)", rabbitUrl));
            return Response.status(HttpStatus.SC_OK).build();

        } catch (DeclarationException e) {
            LOG.error(String.format("Declaration exception :: %s", e.getMessage()));
            LOG.debug(e);

            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(ERROR_MSG).build();
        } catch (ImagePayloadException e) {
            LOG.error(String.format("Image payload exception :: %s", e.getMessage()));
            LOG.debug(e);

            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();

        } catch (EventsManagerException e) {
            LOG.error(String.format("Publishing events manager exception :: %s", e.getMessage()));
            LOG.debug(e);

            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();

        } catch (JsonProcessingException e) {
            LOG.error(String.format("Json payload builder exception :: %s", e.getMessage()));
            LOG.debug(e);

            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();
        }
    }

    private String drsDispatchPayload(ImagePayload imagePayload) throws JsonProcessingException, EventsManagerException {
        FitnoteMetadata metadata = new FitnoteMetadata();
        metadata.setNino(imagePayload.getNinoObject());
        metadata.setBusinessUnitID("35");
        metadata.setDocumentType(8606);
        metadata.setClassification(1);
        metadata.setDocumentSource(4);
        metadata.setBenefitType(37);

        if (imagePayload.getClaimantAddress() != null) {
            metadata.setPostCode(imagePayload.getClaimantAddress().getPostcode());
            metadata.setOfficePostcode(esaGetOfficePostcode());
        }

        if ((imagePayload.getMobileNumber() != null) && (!imagePayload.getMobileNumber().trim().isEmpty())) {
            metadata.setCustomerMobileNumber(imagePayload.getMobileNumber());
        }

        UUID correlationId = UUID.randomUUID();
        AMQP.BasicProperties extraSettings = new AMQP.BasicProperties().builder().correlationId(correlationId.toString()).build();
        String payload = new DrsPayloadBuilder<ImagePayload, FitnoteMetadata>().getDrsPayloadJson(imagePayload, metadata);

        try {
            rabbitMqPublish.publishBasicTextMessageToExchange(config.getRabbitExchangeName(), config.getEventRoutingKey(), payload, extraSettings);

        } catch (URISyntaxException | TimeoutException | NoSuchAlgorithmException | KeyManagementException |
                IOException | UnrecoverableKeyException | CertificateException | KeyStoreException |
                TLSGeneralException | EventsManagerException e) {

            LOG.error(String.format("Publishing error %s :: %s", e.getClass().getName(), e.getMessage()));
            LOG.debug(e);

            throw new EventsManagerException(e.getMessage());
        }

        return correlationId.toString();
    }

    private String esaGetOfficePostcode() {
        return config.getEsaDefaultRoutingForDRS();
    }
}
