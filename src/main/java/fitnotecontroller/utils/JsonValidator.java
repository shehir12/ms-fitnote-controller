package fitnotecontroller.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fitnotecontroller.domain.Address;
import fitnotecontroller.domain.Declaration;
import fitnotecontroller.domain.ImagePayload;
import fitnotecontroller.exception.DeclarationException;
import fitnotecontroller.exception.ImagePayloadException;
import fitnotecontroller.exception.NewAddressException;
import fitnotecontroller.exception.ObjectBuildException;
import gov.dwp.utilities.formatvalidation.NinoValidator;
import gov.dwp.utilities.formatvalidation.PostCodeValidator;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.log4j.Logger;

import java.io.IOException;

public class JsonValidator {
    private static final Logger LOG = DwpEncodedLogger.getLogger(JsonValidator.class.getName());
    private static final String SESSION_ID_MISSING = "Session ID missing";
    private static final String MISSING_ACCEPTED_VALUED = "Missing accepted valued";
    private static final String INVALID_VALUE = "Invalid value";
    static final String SESSION_ID_IS_MANDATORY = "Session ID is mandatory";
    static final String INVALID_NINO = "Invalid Nino";
    static final String INVALID_MOBILE_NUMBER = "Invalid Mobile Number";
    static final String MISSING_NUMBER_FROM_ADDRESS = "Missing number from address";
    static final String HOUSE_NUMBER_LENGTH_EXCEPTION = "House number length exception";
    static final String STREET_NAME_LENGTH_EXCEPTION = "Street name length exception";
    static final String CITY_NAME_LENGTH_EXCEPTION = "City name length exception";
    static final String POSTCODE_VALIDATION_FAILED = "Postcode validation failed";
    static final String NO_JSON = "No Json";

    public ImagePayload validateAndTranslateSubmission(String json) throws ImagePayloadException {
        ImagePayload payload;
        try {
            payload = buildObjectFromJson(json, ImagePayload.class);
            if (invalidString(payload.getSessionId())) {
                LOG.debug(SESSION_ID_MISSING);
                throw new ImagePayloadException(SESSION_ID_IS_MANDATORY);
            }
            payload.setFitnoteCheckStatus(ImagePayload.Status.UPLOADED);
        } catch (ObjectBuildException | IOException e) {
            throw new ImagePayloadException(e);
        }

        return payload;
    }

    public ImagePayload validateAndTranslateBarcodeSubmission(String json) throws ImagePayloadException {
        ImagePayload payload;
        try {
            payload = buildObjectFromJson(json, ImagePayload.class);
            if (invalidString(payload.getSessionId())) {
                LOG.debug(SESSION_ID_MISSING);
                throw new ImagePayloadException(SESSION_ID_IS_MANDATORY);
            }
            payload.setBarcodeCheckStatus(ImagePayload.Status.UPLOADED);
        } catch (ObjectBuildException | IOException e) {
            throw new ImagePayloadException(e);
        }

        return payload;
    }

    public ImagePayload validateAndTranslateConfirmation(String json) throws ImagePayloadException {
        ImagePayload payload;
        try {
            payload = buildObjectFromJson(json, ImagePayload.class);
            if (invalidString(payload.getSessionId())) {
                throw new ImagePayloadException(SESSION_ID_IS_MANDATORY);
            } else if (!NinoValidator.validateNINO(payload.getNino())) {
                throw new ImagePayloadException(INVALID_NINO);
            }
        } catch (ObjectBuildException | IOException e) {
            throw new ImagePayloadException(e);
        }
        return payload;
    }

    public ImagePayload validateAndTranslateMobileConfirmation(String json) throws ImagePayloadException {
        ImagePayload payload;
        try {
            payload = buildObjectFromJson(json, ImagePayload.class);
            if (invalidString(payload.getSessionId())) {
                throw new ImagePayloadException(SESSION_ID_IS_MANDATORY);
            }//may need better validation?
            if ((!invalidString(payload.getMobileNumber())) && (!payload.getMobileNumber().matches("^\\+?[ 0-9]{9,15}"))) {
                throw new ImagePayloadException(INVALID_MOBILE_NUMBER);
            }
        } catch (ObjectBuildException | IOException e) {
            throw new ImagePayloadException(e);
        }
        return payload;
    }

    public Declaration validateAndTranslateDeclaration(String json) throws DeclarationException {
        Declaration declaration;
        try {
            declaration = buildObjectFromJson(json, Declaration.class);
            if (invalidString(declaration.getSessionId())) {
                throw new DeclarationException(SESSION_ID_IS_MANDATORY);
            } else if (declaration.isAccepted() == null) {
                throw new DeclarationException(MISSING_ACCEPTED_VALUED);
            }

            if (!declaration.isAccepted()) {
                throw new DeclarationException(INVALID_VALUE);
            }

        } catch (ObjectBuildException | IOException e) {
            throw new DeclarationException(e);
        }
        return declaration;
    }

    public Address validateAndTranslateAddress(String json) throws NewAddressException {
        Address addressReceived;
        try {
            addressReceived = buildObjectFromJson(json, Address.class);
            if (invalidString(addressReceived.getSessionId())) {
                throw new NewAddressException(SESSION_ID_IS_MANDATORY);
            } else if (invalidString(addressReceived.getHouseNameOrNumber())) {
                throw new NewAddressException(MISSING_NUMBER_FROM_ADDRESS);
            }

            if (invalidOverLengthCheck(addressReceived.getHouseNameOrNumber())) {
                throw new NewAddressException(HOUSE_NUMBER_LENGTH_EXCEPTION);
            } else if (invalidOverLengthCheck(addressReceived.getStreet())) {
                throw new NewAddressException(STREET_NAME_LENGTH_EXCEPTION);
            } else if (invalidOverLengthCheck(addressReceived.getCity())) {
                throw new NewAddressException(CITY_NAME_LENGTH_EXCEPTION);
            }

            if (!PostCodeValidator.validateInput(addressReceived.getPostcode())) {
                throw new NewAddressException(POSTCODE_VALIDATION_FAILED);
            }

        } catch (ObjectBuildException | IOException e) {
            throw new NewAddressException(e);
        }
        return addressReceived;
    }

    private <T> T buildObjectFromJson(String json, Class<T> objectToBuild) throws IOException, ObjectBuildException {
        if (json == null) {
            throw new ObjectBuildException(NO_JSON);
        }
        return new ObjectMapper().readValue(json, objectToBuild);
    }

    private boolean invalidString(String str) {
        return str == null || str.trim().length() == 0;
    }

    private boolean invalidOverLengthCheck(String str) {
        boolean isInvalid = false;

        if (!invalidString(str)) {
            isInvalid = str.length() > 35;
        }

        return isInvalid;
    }
}