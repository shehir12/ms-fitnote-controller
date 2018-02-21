package fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import fitnotecontroller.domain.ImagePayload;
import fitnotecontroller.domain.Views;
import fitnotecontroller.exception.ImagePayloadException;
import fitnotecontroller.utils.JsonValidator;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class FitnoteConfirmationResource {
    private static final Logger LOG = DwpEncodedLogger.getLogger(FitnoteConfirmationResource.class.getName());
    private static final String ERROR_MSG = "Unable to process request";
    private ImageStorage imageStorage;
    private JsonValidator validator;


    public FitnoteConfirmationResource(ImageStorage imageStorage) {
        this(imageStorage, new JsonValidator());
    }

    public FitnoteConfirmationResource(ImageStorage imageStorage, JsonValidator validator) {
        this.imageStorage = imageStorage;
        this.validator = validator;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/nino")
    public Response confirmFitnote(String json) {
        Response response;
        try {
            ImagePayload imagePayload = validator.validateAndTranslateConfirmation(json);
            LOG.debug("Json validated");
            imageStorage.updateNinoDetails(imagePayload);
            LOG.info("NINO updated");
            response = createResponseOf(HttpStatus.SC_OK, createResponseFrom(imagePayload));
        } catch (ImagePayloadException e) {
            LOG.debug("Unable to process request examining payload", e);
            response = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_MSG);
        } catch (JsonProcessingException e) {
            response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_MSG);
            LOG.debug(ERROR_MSG, e);
        }
        return response;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/mobile")
    public Response confirmMobile(String json) {
        Response response;
        try {
            ImagePayload imagePayload = validator.validateAndTranslateMobileConfirmation(json);
            LOG.debug("Json validated");
            ImagePayload payload = imageStorage.updateMobileDetails(imagePayload);
            imageStorage.updateMobileDetails(payload);
            LOG.info("Mobile number updated");
            response = createResponseOf(HttpStatus.SC_OK, createResponseFrom(imagePayload));
        } catch (ImagePayloadException e) {
            LOG.debug("Unable to process request when examining payload", e);
            response = createResponseOf(HttpStatus.SC_BAD_REQUEST, ERROR_MSG);
        } catch (JsonProcessingException e) {
            response = createResponseOf(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_MSG);
            LOG.debug(ERROR_MSG, e);
        }
        return response;
    }

    private String createResponseFrom(ImagePayload payload) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        return mapper.writerWithView(Views.SessionOnly.class).writeValueAsString(payload);
    }

    private Response createResponseOf(int status, String message) {
        return Response.status(status).entity(message).build();
    }
}
