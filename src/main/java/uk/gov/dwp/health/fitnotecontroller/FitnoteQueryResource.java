package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.Views;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class FitnoteQueryResource {
    private static final Logger LOG = LoggerFactory.getLogger(FitnoteQueryResource.class.getName());
    private static final String IMAGE_ERROR = "ImagePayloadException :: {}";
    private static final String MULTI_ERROR_FORMAT = "Exception :: {}, {}";
    private static final String LOG_STANDARD_REGEX = "[\\u0000-\\u001f]";
    private static final String ERROR_MSG = "Unable to process request";

    private ImageStorage imageStorage;

    public FitnoteQueryResource(ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    @POST
    @Path("/queryNino")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryNino(String json) {
        Response response;
        try {
            ImagePayload payload = imageStorage.getPayload(testAndSanitiseSession(json));

            if (payload.getNino() == null) {
                LOG.debug("null nino value updated to empty string for inclusion to serialisation");
                payload.setNino("");
            }

            String serialisedClass = serialiseSpecificPayloadItems(Views.QueryNinoDetails.class, payload);
            LOG.info("serialised nino query for {}", payload.getSessionId());

            response = Response.status(HttpStatus.SC_OK).entity(serialisedClass).build();

        } catch (IOException | CryptoException e) {
            response = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();
            LOG.error(MULTI_ERROR_FORMAT, e.getClass().getName(), e.getMessage());
            LOG.debug(ERROR_MSG, e);

        } catch (ImagePayloadException e) {
            response = Response.status(HttpStatus.SC_BAD_REQUEST).entity(ERROR_MSG).build();
            LOG.error(IMAGE_ERROR, e.getMessage());
            LOG.debug(ERROR_MSG, e);
        }

        return response;
    }

    @POST
    @Path("/queryAddress")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryAddress(String json) {
        Response response;
        try {
            ImagePayload payload = imageStorage.getPayload(testAndSanitiseSession(json));

            if (payload.getClaimantAddress() == null) {
                LOG.debug("null claimantAddress value updated to empty string for inclusion to serialisation");
                payload.setClaimantAddress(new Address());
            }

            String serialisedClass = serialiseSpecificPayloadItems(Views.QueryAddressDetails.class, payload);
            LOG.info("serialised claimantAddress query for {}", payload.getSessionId());

            response = Response.status(HttpStatus.SC_OK).entity(serialisedClass).build();

        } catch (IOException | CryptoException e) {
            response = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();
            LOG.error(MULTI_ERROR_FORMAT, e.getClass().getName(), e.getMessage());
            LOG.debug(ERROR_MSG, e);

        } catch (ImagePayloadException e) {
            response = Response.status(HttpStatus.SC_BAD_REQUEST).entity(ERROR_MSG).build();
            LOG.error(IMAGE_ERROR, e.getMessage());
            LOG.debug(ERROR_MSG, e);
        }

        return response;
    }

    @POST
    @Path("/queryMobile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryMobile(String json) {
        Response response;
        try {
            ImagePayload payload = imageStorage.getPayload(testAndSanitiseSession(json));

            if (payload.getMobileNumber() == null) {
                LOG.debug("null mobileNumber value updated to empty string for inclusion to serialisation");
                payload.setMobileNumber("");
            }

            String serialisedClass = serialiseSpecificPayloadItems(Views.QueryMobileDetails.class, payload);
            LOG.info("serialised mobileNumber query for {}", payload.getSessionId());

            response = Response.status(HttpStatus.SC_OK).entity(serialisedClass).build();

        } catch (IOException | CryptoException e) {
            response = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_MSG).build();
            LOG.error(MULTI_ERROR_FORMAT, e.getClass().getName(), e.getMessage());
            LOG.debug(ERROR_MSG, e);

        } catch (ImagePayloadException e) {
            response = Response.status(HttpStatus.SC_BAD_REQUEST).entity(ERROR_MSG).build();
            LOG.error(IMAGE_ERROR, e.getMessage());
            LOG.debug(ERROR_MSG, e);
        }

        return response;
    }

    private String testAndSanitiseSession(String json) throws ImagePayloadException, IOException {
        if ((json == null) || (json.isEmpty())) {
            throw new ImagePayloadException("json cannot be empty or null");
        }

        JsonNode jsonNode = new ObjectMapper().readTree(json);
        return jsonNode.get("sessionId") != null? jsonNode.get("sessionId").textValue().replaceAll(LOG_STANDARD_REGEX, "") : null;
    }

    private String serialiseSpecificPayloadItems(Class<?> mapperView, ImagePayload payload) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        return mapper.writerWithView(mapperView).writeValueAsString(payload);
    }
}
