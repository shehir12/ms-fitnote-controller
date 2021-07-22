package uk.gov.dwp.health.fitnotecontroller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.domain.Views;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class FitnoteConfirmationResource extends AbstractResource {
  private static final Logger LOG =
      LoggerFactory.getLogger(FitnoteConfirmationResource.class.getName());

  public FitnoteConfirmationResource(ImageStorage imageStore) {
    super(imageStore);
  }

  public FitnoteConfirmationResource(ImageStorage imageStore, JsonValidator jsonValidator) {
    super(imageStore, jsonValidator);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/nino")
  public Response confirmFitnote(String json) {
    Response response;
    try {
      ImagePayload imagePayload = jsonValidator.validateAndTranslateConfirmation(json);
      LOG.debug("Json validated");
      imageStore.updateNinoDetails(imagePayload);

      LOG.info("NINO updated");
      response = createResponseOf(HttpStatus.SC_OK, createResponseFrom(imagePayload));

    } catch (ImagePayloadException e) {
      LOG.debug("Unable to process request examining payload", e);
      return createImage400ErrorResponse(e, LOG);

    } catch (IOException e) {
      return createIOErrorResponse(e, LOG);

    } catch (CryptoException e) {
      return createCrypto500Response(e, LOG);
    }
    return response;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/mobile")
  public Response confirmMobile(String json) {
    Response response;
    try {
      ImagePayload imagePayload = jsonValidator.validateAndTranslateMobileConfirmation(json);
      LOG.debug("Json validated");

      imageStore.updateMobileDetails(imagePayload);
      LOG.info("Mobile number updated");

      response = createResponseOf(HttpStatus.SC_OK, createResponseFrom(imagePayload));

    } catch (ImagePayloadException e) {
      LOG.debug("Unable to process request when examining payload", e);
      return createImage400ErrorResponse(e, LOG);

    } catch (CryptoException e) {
      response = createCrypto400Response(e, LOG);

    } catch (IOException e) {
      return createIOErrorResponse(e, LOG);
    }
    return response;
  }

  private String createResponseFrom(ImagePayload payload) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    return mapper.writerWithView(Views.SessionOnly.class).writeValueAsString(payload);
  }
}
