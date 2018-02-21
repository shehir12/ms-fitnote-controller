package fitnotecontroller;

import fitnotecontroller.domain.Address;
import fitnotecontroller.domain.ImagePayload;
import fitnotecontroller.exception.NewAddressException;
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
public class FitnoteAddressResource {
    private static final Logger LOG = DwpEncodedLogger.getLogger(FitnoteAddressResource.class.getName());
    private static final String ERROR_RESPONSE = "Unable to process request";
    private JsonValidator jsonValidator;
    private ImageStorage imageStore;

    public FitnoteAddressResource(ImageStorage imageStore) {
        this(imageStore, new JsonValidator());
    }

    public FitnoteAddressResource(ImageStorage imageStore, JsonValidator jsonValidator) {
        this.jsonValidator = jsonValidator;
        this.imageStore = imageStore;
    }

    @POST
    @Path("/address")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAddress(String jsonBody) {
        Response serviceResponse;
        try {
            Address receivedAddress = jsonValidator.validateAndTranslateAddress(jsonBody);
            ImagePayload payload = imageStore.getPayload(receivedAddress.getSessionId());
            payload.setClaimantAddress(receivedAddress);
            serviceResponse = Response.status(HttpStatus.SC_OK).build();
        } catch (NewAddressException e) {
            serviceResponse = Response.status(HttpStatus.SC_BAD_REQUEST).entity(ERROR_RESPONSE).build();
            LOG.debug(e);
        } catch (Exception e) {
            serviceResponse = Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(ERROR_RESPONSE).build();
            LOG.debug(e);
        }
        return serviceResponse;
    }
}
