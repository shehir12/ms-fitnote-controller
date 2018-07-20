package uk.gov.dwp.health.fitnotecontroller.utils;

import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.DeclarationException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.exception.NewAddressException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.NotNull;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JsonValidatorTest extends JsonValidator {
    private static final String JPG_IMAGE_LANDSCAPE = "src/test/resources/FullPage_Landscape.jpg";
    private static final String DATAMATRIX_IMAGE = "src/test/resources/working_barcode.jpg";
    private String ENCODED_DATAMATRIX_STRING = null;
    private String ENCODED_LANDSCAPE_STRING = null;

    private static final String INVALID_HOUSE_LENGTH = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"123456789012345678901234567890123456\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String INVALID_STREET_LENGTH = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"123456789012345678901234567890123456\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String INVALID_CITY_LENGTH = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"123456789012345678901234567890123456\", \"postcode\" : \"NE12 9LG\"}";
    private static final String INVALID_POSTCODE_LENGTH = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"123456789012345678901234567890123456\"}";
    private static final String VALID_ADDRESS = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_SESSION_ID = "{ \"nameOrumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_NUMBER = "{ \"sessionId\" :\"123456\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_STREET = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\" , \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_CITY = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\",  \"postcode\" : \"NE12 9LG\"}";
    private static final String ADDRESS_MISSING_POSTCODE = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\"}";
    private String BARCODE_SUBMIT_VALID = "{ \"sessionId\" : \"123456\", \"barcodeImage\" : \"%s\"}";
    private String BARCODE_SUBMIT_EMPTY_SESSION_ID = "{ \"sessionId\" : \"\", \"barcodeImage\" : \"%s\"}";
    private String BARCODE_SUBMIT_MISSING_SESSION_ID = "{ \"barcodeImage\" : \"%s\"}";

    private JsonValidator validatorUnderTest = new JsonValidator();

    @Before
    public void setup() throws IOException {
        ENCODED_LANDSCAPE_STRING = Base64.encodeBase64String(FileUtils.readFileToByteArray(new File(JPG_IMAGE_LANDSCAPE)));
        ENCODED_DATAMATRIX_STRING = Base64.encodeBase64String(FileUtils.readFileToByteArray(new File(DATAMATRIX_IMAGE)));

        BARCODE_SUBMIT_VALID = String.format(BARCODE_SUBMIT_VALID, ENCODED_DATAMATRIX_STRING);
        BARCODE_SUBMIT_MISSING_SESSION_ID = String.format(BARCODE_SUBMIT_MISSING_SESSION_ID, ENCODED_DATAMATRIX_STRING);
        BARCODE_SUBMIT_EMPTY_SESSION_ID = String.format(BARCODE_SUBMIT_EMPTY_SESSION_ID, ENCODED_DATAMATRIX_STRING);
    }

    @Test
    public void imagePayloadObjectIsReturnedForValidJsonOnSubmission() throws ImagePayloadException {
        String sessionId = "123456";
        String json = "{\"image\":\"" + ENCODED_LANDSCAPE_STRING + "\",\"sessionId\":\"" + sessionId + "\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateSubmission(json);
        assertThat(imagePayload, is(notNullValue()));
        assertThat(imagePayload.getImage(), is(ENCODED_LANDSCAPE_STRING));
        assertThat(imagePayload.getSessionId(), is(sessionId));
    }

    @Test
    public void imagePayload_Object_Is_Returned_For_Valid_Json() throws ImagePayloadException, IOException {
        String sessionId = "123456";
        String json = "{\"sessionId\":\"" + sessionId + "\"," +
                "\"nino\":\"AA370773A\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateConfirmation(json);
        assertThat(imagePayload, is(notNullValue()));
        assertThat(imagePayload.getSessionId(), is(sessionId));
    }

    @Test
    public void null_SessionId_On_Submission_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateSubmission(("{\"image\":\"" + "678678678678678fuyff" + "\"}"));
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
            e.printStackTrace();
        }
    }

    @Test
    public void null_Json_On_Submission_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateSubmission(null);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(NO_JSON));
            e.printStackTrace();
        }
    }

    @Test
    public void empty_SessionId_On_Submission_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateSubmission(("{\"image\":\"" + "678678678678678fuyff" + "\",\"sessionId\":\"\"}"));
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
            e.printStackTrace();
        }
    }

    @Test
    public void invalid_Json_On_Submission_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateSubmission("invalid json");
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains("JsonParseException"));
            e.printStackTrace();
        }
    }

    @Test
    public void null_SessionId_On_Confirmation_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateSubmission(("{\"nino\":\"AA370773A\"," +
                    "\"mobileNumber\":\"0113999999\"}"));
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
            e.printStackTrace();
        }
    }

    @Test
    public void empty_SessionId_On_Confirmation_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateConfirmation("{\"sessionId\":\"\"," +
                    "\"nino\":\"AA370773A\"," +
                    "\"mobileNumber\":\"0113999999\"}");
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
            e.printStackTrace();
        }
    }

    @Test
    public void invalid_Json_On_Confirmation_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateConfirmation("invalid json");
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains("JsonParseException"));
            e.printStackTrace();
        }
    }

    @Test
    public void empty_NINO_On_Confirmation_Throws_Exception() {
        try {
            String json = "{\"sessionId\":\"123456\"," +
                    "\"nino\":\"\"}";
            validatorUnderTest.validateAndTranslateConfirmation(json);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(INVALID_NINO));
            e.printStackTrace();
        }
    }

    @Test
    public void missing_NINO_On_Confirmation_Throws_Exception() {
        try {
            String json = "{\"sessionId\":\"123456\"}";
            validatorUnderTest.validateAndTranslateConfirmation(json);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(INVALID_NINO));
            e.printStackTrace();
        }
    }

    @Test
    public void null_Json_On_Confirmation_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateConfirmation(null);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(NO_JSON));
            e.printStackTrace();
        }
    }

    @Test
    public void json_On_Mobile_Confirmation_Is_Valid() throws ImagePayloadException {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\",\"mobileNumber\":\"07869123456\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), SessionID);
    }

    @Test
    public void json_On_Mobile_Confirmation_Is_Valid_With_Leading_Plus() throws ImagePayloadException {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\",\"mobileNumber\":\"+44113999999\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), SessionID);
    }

    @Test
    public void json_On_Mobile_Confirmation_Is_Invalid_8_Digits() {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\",\"mobileNumber\":\"12345678\"}";
        try {
            validatorUnderTest.validateAndTranslateMobileConfirmation(json);
            fail("should have thrown invalid mobile number error");
        } catch (ImagePayloadException e) {
            assertTrue("expecting missing mobile number", e.getMessage().contains(INVALID_MOBILE_NUMBER));
            e.printStackTrace();
        }
    }

    @Test
    public void json_On_Mobile_Confirmation_Is_Valid_With_11_Digits() throws ImagePayloadException {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\",\"mobileNumber\":\"12345678901\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), SessionID);
    }

    @Test
    public void json_On_Mobile_Confirmation_Is_Valid_With_15_Digits() throws ImagePayloadException {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\",\"mobileNumber\":\"111111111111111\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), SessionID);
    }

    @Test
    public void json_On_Mobile_Confirmation_Is_Invalid_With_21_Digits() {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\",\"mobileNumber\":\"111111111111111111111\"}";
        try {
            validatorUnderTest.validateAndTranslateMobileConfirmation(json);
            fail("should have thrown invalid mobile number error");
        } catch (ImagePayloadException e) {
            assertTrue("expecting missing mobile number", e.getMessage().contains(INVALID_MOBILE_NUMBER));
            e.printStackTrace();
        }
    }


    @Test
    public void json_On_Mobile_Confirmation_Is_Invalid_With_Plus_Mid_String() {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\",\"mobileNumber\":\"01139+99999\"}";
        try {
            validatorUnderTest.validateAndTranslateMobileConfirmation(json);
            fail("should have thrown invalid mobile number error");
        } catch (ImagePayloadException e) {
            assertTrue("expecting missing mobile number", e.getMessage().contains(INVALID_MOBILE_NUMBER));
            e.printStackTrace();
        }
    }

    @Test
    public void json_On_Mobile_Confirmation_With_No_String_Is_Valid() throws ImagePayloadException {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\",\"mobileNumber\":\"\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), SessionID);
    }

    @Test
    public void json_On_Mobile_Confirmation_With_No_Second_Data() throws ImagePayloadException {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\"}";
        ImagePayload imagePayload = validatorUnderTest.validateAndTranslateMobileConfirmation(json);
        assertEquals(imagePayload.getSessionId(), SessionID);
    }

    @Test
    public void json_On_Mobile_Confirmation_With_Mobile_Number_As_Text() {
        String SessionID = "10";
        String json = "{\"sessionId\":\"" + SessionID + "\",\"mobileNumber\":\"A test message that should fail\"}";

        try {
            validatorUnderTest.validateAndTranslateMobileConfirmation(json);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(INVALID_MOBILE_NUMBER));
            e.printStackTrace();
        }
    }


    @Test
    public void empty_SessionId_On_Declaration_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateDeclaration("{\"sessionId\":\"\",\"accepted\" : false}");
            fail("Should have thrown a DeclarationException");
        } catch (DeclarationException e) {
            assertTrue("Expecting a DeclarationException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
            e.printStackTrace();
        }
    }

    @Test
    public void invalid_Json_On_Declaration_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateDeclaration("Invalid json");
            fail("Should have thrown a DeclarationException");
        } catch (DeclarationException e) {
            assertTrue("Expecting a DeclarationException", e.getMessage().contains("JsonParseException"));
            e.printStackTrace();
        }
    }

    @Test
    public void null_Json_On_Declaration_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateDeclaration(null);
            fail("Should have thrown a DeclarationException");
        } catch (DeclarationException e) {
            assertTrue("Expecting a DeclarationException", e.getMessage().contains(NO_JSON));
            e.printStackTrace();
        }
    }

    @Test(expected = NewAddressException.class)
    public void nullAddressThrowsException() throws NewAddressException {
        validatorUnderTest.validateAndTranslateAddress(null);
    }

    @Test
    public void empty_SessionId_On_Address_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_SESSION_ID);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains("Unrecognized field"));
            e.printStackTrace();
        }
    }

    @Test
    public void empty_Number_On_Address_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_NUMBER);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(MISSING_NUMBER_FROM_ADDRESS));
            e.printStackTrace();
        }
    }

    @Test
    public void houseNameOrNumber_Longer_Than_35_Chars() {
        try {
            validatorUnderTest.validateAndTranslateAddress(INVALID_HOUSE_LENGTH);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(HOUSE_NUMBER_LENGTH_EXCEPTION));
            e.printStackTrace();
        }
    }

    @Test
    public void streetName_Longer_Than_35_Chars() {
        try {
            validatorUnderTest.validateAndTranslateAddress(INVALID_STREET_LENGTH);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(STREET_NAME_LENGTH_EXCEPTION));
            e.printStackTrace();
        }
    }

    @Test
    public void city_Longer_Than_35_Chars() {
        try {
            validatorUnderTest.validateAndTranslateAddress(INVALID_CITY_LENGTH);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(CITY_NAME_LENGTH_EXCEPTION));
            e.printStackTrace();
        }
    }

    @Test
    public void postcode_Longer_Than_35_Chars() {
        try {
            validatorUnderTest.validateAndTranslateAddress(INVALID_POSTCODE_LENGTH);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(POSTCODE_VALIDATION_FAILED));
            e.printStackTrace();
        }
    }

    @Test
    public void empty_Street_On_Address_Does_Not_Throw_Exception() throws NewAddressException {
        validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_STREET);
    }

    @Test
    public void empty_City_On_Address_Does_Not_Throw_Exception() throws NewAddressException {
        validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_CITY);
    }

    @Test
    public void empty_Postcode_On_Address_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateAddress(ADDRESS_MISSING_POSTCODE);
            fail("Should have thrown a NewAddressException");
        } catch (NewAddressException e) {
            assertTrue("Expecting a NewAddressException", e.getMessage().contains(POSTCODE_VALIDATION_FAILED));
            e.printStackTrace();
        }
    }

    @Test
    public void valid_Json_Returns_Correct_Address() throws NewAddressException {
        Address addressReturned = validatorUnderTest.validateAndTranslateAddress(VALID_ADDRESS);
        assertThat(addressReturned.getSessionId(), is("123456"));
        assertThat(addressReturned.getHouseNameOrNumber(), is("254"));
        assertThat(addressReturned.getStreet(), is("Bakers Street"));
        assertThat(addressReturned.getCity(), is("London"));
        assertThat(addressReturned.getPostcode(), is("NE12 9LG"));
    }


    @Test
    public void valid_QR_Submission_Returns_Payload() throws ImagePayloadException {
        ImagePayload payload = validatorUnderTest.validateAndTranslateBarcodeSubmission(BARCODE_SUBMIT_VALID);
        assertThat(payload, is(NotNull.NOT_NULL));
        assertThat(payload.getSessionId(), is("123456"));
        assertThat(payload.getBarcodeImage(), is(NotNull.NOT_NULL));
    }

    @Test
    public void empty_SessionId_On_QR_Submission_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateBarcodeSubmission(BARCODE_SUBMIT_EMPTY_SESSION_ID);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
            e.printStackTrace();
        }
    }

    @Test
    public void missing_SessionId_On_QR_Submission_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateBarcodeSubmission(BARCODE_SUBMIT_MISSING_SESSION_ID);
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
            e.printStackTrace();
        }

    }

    @Test
    public void invalid_Json_On_QR_Submission_Throws_Exception() {
        try {
            validatorUnderTest.validateAndTranslateBarcodeSubmission("{}");
            fail("Should have thrown an ImagePayloadException");
        } catch (ImagePayloadException e) {
            assertTrue("Expecting an ImagePayloadException", e.getMessage().contains(SESSION_ID_IS_MANDATORY));
            e.printStackTrace();
        }
    }

}