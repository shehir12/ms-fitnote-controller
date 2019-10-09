package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.crypto.exceptions.EventsMessageException;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.Declaration;
import uk.gov.dwp.health.fitnotecontroller.domain.FitnoteMetadata;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.DeclarationException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import uk.gov.dwp.components.drs.DrsPayloadBuilder;
import uk.gov.dwp.health.messageq.amazon.sns.MessagePublisher;
import uk.gov.dwp.health.messageq.exceptions.EventsManagerException;
import uk.gov.dwp.health.messageq.items.event.EventMessage;
import uk.gov.dwp.health.messageq.items.event.MetaData;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.UUID;

@Path("/")
public class FitnoteDeclarationResource {
    private static final Logger LOG = LoggerFactory.getLogger(FitnoteDeclarationResource.class.getName());
    private static final String ERROR_MSG = "Unable to process request";
    private FitnoteControllerConfiguration config;
    private MessagePublisher snsPublisher;
    private JsonValidator jsonValidator;
    private ImageStorage imageStore;

    public FitnoteDeclarationResource(ImageStorage imageStore,
                                      MessagePublisher snsPublisher,
                                      FitnoteControllerConfiguration config) {
        this(imageStore, new JsonValidator(), snsPublisher, config);
    }

    public FitnoteDeclarationResource(ImageStorage imageStore,
                                      JsonValidator jsonValidator,
                                      MessagePublisher snsPublisher,
                                      FitnoteControllerConfiguration config) {
        this.jsonValidator = jsonValidator;
        this.snsPublisher = snsPublisher;
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
                LOG.debug("REJECT :: no image data to process for session {}", declaration.getSessionId());
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Cannot process declaration without a Fitnote Image").build();
            }

            if (!imagePayloadToSubmit.getFitnoteCheckStatus().equals(ImagePayload.Status.SUCCEEDED)) {
                LOG.debug("REJECT :: the fitnote image was not scanned successfully for session {}", declaration.getSessionId());
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Cannot process declaration without a successfully scanned Fitnote Image").build();
            }

            if ((imagePayloadToSubmit.getNino() == null) || (imagePayloadToSubmit.getNino().isEmpty())) {
                LOG.debug("REJECT :: no NINO specified for session {}", declaration.getSessionId());
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Cannot process declaration without a valid NINO").build();
            }

            if (imagePayloadToSubmit.getClaimantAddress() == null) {
                LOG.debug("REJECT :: Claimant address has not been specified for session {}", declaration.getSessionId());
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Address must be specified").build();
            }

            LOG.debug("Address on fitnote is supplied");
            LOG.debug("Session Id {} is ok to process", declaration.getSessionId());
            LOG.info("Post image payload directly to SNS topic '{}'", config.getSnsTopicName());
            String transactionId = drsDispatchPayload(imagePayloadToSubmit);

            LOG.info("Clear all data for session {} with correlation id '{}'", declaration.getSessionId(), transactionId);
            imageStore.clearSession(declaration.getSessionId());

            LOG.info("Successfully posted image data to SNS topic ({})", config.getSnsTopicName());
            return Response.status(HttpStatus.SC_OK).build();

        } catch (DeclarationException e) {
            LOG.error("Declaration exception :: {}", e.getMessage());
            LOG.debug(e.getClass().getName(), e);

            return Response.status(HttpStatus.SC_BAD_REQUEST).entity(ERROR_MSG).build();

        } catch (CryptoException e) {
            LOG.error("CryptoException exception :: {}", e.getMessage());
            LOG.debug(e.getClass().getName(), e);

            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();

        } catch (ImagePayloadException e) {
            LOG.error("Image payload exception :: {}", e.getMessage());
            LOG.debug(e.getClass().getName(), e);

            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();

        } catch (EventsManagerException e) {
            LOG.error("Publishing events manager exception :: {}", e.getMessage());
            LOG.debug(e.getClass().getName(), e);

            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();

        } catch (IOException e) {
            LOG.error("Json payload builder exception :: {}", e.getMessage());
            LOG.debug(e.getClass().getName(), e);

            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();
        }
    }

    private String drsDispatchPayload(ImagePayload imagePayload) throws IOException, EventsManagerException {
        FitnoteMetadata drsMetadata = new FitnoteMetadata();
        ObjectMapper mapper = new ObjectMapper();

        drsMetadata.setNino(imagePayload.getNinoObject());
        drsMetadata.setBusinessUnitID("35");
        drsMetadata.setDocumentType(8606);
        drsMetadata.setClassification(1);
        drsMetadata.setDocumentSource(4);
        drsMetadata.setBenefitType(37);

        if (imagePayload.getClaimantAddress() != null) {
            drsMetadata.setPostCode(imagePayload.getClaimantAddress().getPostcode());
        }

        if ((imagePayload.getMobileNumber() != null) && (!imagePayload.getMobileNumber().trim().isEmpty())) {
            drsMetadata.setCustomerMobileNumber(imagePayload.getMobileNumber());
        }

        UUID correlationId = UUID.randomUUID();

        MetaData metaData = new MetaData(Collections.singletonList(config.getSnsSubject()));
        metaData.setRoutingKey(config.getSnsRoutingKey());
        metaData.setCorrelationId(correlationId.toString());

        EventMessage messageQueueEvent = new EventMessage();
        messageQueueEvent.setMetaData(metaData);
        messageQueueEvent.setBodyContents(mapper.readValue(new DrsPayloadBuilder<ImagePayload, FitnoteMetadata>().getDrsPayloadJson(imagePayload, drsMetadata), Object.class));

        try {
            snsPublisher.publishMessageToSnsTopic(config.isSnsEncryptMessages(), config.getSnsTopicName(), config.getSnsSubject(), messageQueueEvent, null);

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                InvocationTargetException | EventsMessageException | CryptoException e) {

            LOG.error("Publishing error {} :: {}", e.getClass().getName(), e.getMessage());
            LOG.debug(e.getClass().getName(), e);

            throw new EventsManagerException(e.getMessage());
        }

        return correlationId.toString();
    }
}
