package fitnotecontroller.domain;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class ContactCentreTest {

    private final String VALID_ESA_LOOKUP_RETURN = "{\"office\":\"ESA Hull\",\"mailsite\":\"Mail Handling Site A\"," +
            "\"postcode\":\"WV98 2AG\",\"benefitCentre\":\"HULL BC\",\"freepostAddress\":\"Freepost DWP ESA 17\"," +
            "\"officeRoutingCodeForDRS\":\"ES1 9QX\"}";

    private final String INVALID_ESA_LOOKUP_RETURN_NULL_OFFICE = "{\"office\":null,\"mailsite\":\"Mail Handling Site A\"," +
            "\"postcode\":\"WV98 2AG\",\"benefitCentre\":\"HULL BC\",\"freepostAddress\":\"Freepost DWP ESA 17\"," +
            "\"officeRoutingCodeForDRS\":\"ES1 9QX\"}";

    private final String INVALID_ESA_LOOKUP_RETURN_EMPTY_OFFICE = "{\"office\":\"\",\"mailsite\":\"Mail Handling Site A\"," +
            "\"postcode\":\"WV98 2AG\",\"benefitCentre\":\"HULL BC\",\"freepostAddress\":\"Freepost DWP ESA 17\"," +
            "\"officeRoutingCodeForDRS\":\"ES1 9QX\"}";

    private final String INVALID_ESA_LOOKUP_RETURN_NULL_DRS_ROUTING = "{\"office\":\"ESA Hull\",\"mailsite\":\"Mail Handling Site A\"," +
            "\"postcode\":\"WV98 2AG\",\"benefitCentre\":\"HULL BC\",\"freepostAddress\":\"Freepost DWP ESA 17\"," +
            "\"officeRoutingCodeForDRS\":null}";

    private final String INVALID_ESA_LOOKUP_RETURN_EMPTY_DRS_ROUTING = "{\"office\":\"ESA Hull\",\"mailsite\":\"Mail Handling Site A\"," +
            "\"postcode\":\"WV98 2AG\",\"benefitCentre\":\"HULL BC\",\"freepostAddress\":\"Freepost DWP ESA 17\"," +
            "\"officeRoutingCodeForDRS\":\"\"}";

    @Test
    public void testValidJsonIsAccepted() throws IOException {
        ContactCentre instance = new ObjectMapper().readValue(VALID_ESA_LOOKUP_RETURN, ContactCentre.class);
        assertThat("json should be valid", instance.isContentValid(), is(true));
    }

    @Test
    public void testBlankJsonIsInvalid() throws IOException {
        ContactCentre instance = new ObjectMapper().readValue("{}", ContactCentre.class);
        assertThat("blank json is invalid", instance.isContentValid(), is(false));
    }

    @Test
    public void testNullValueForOfficeIsInvalid() throws IOException {
        ContactCentre instance = new ObjectMapper().readValue(INVALID_ESA_LOOKUP_RETURN_NULL_OFFICE, ContactCentre.class);
        assertThat("null office value is invalid", instance.isContentValid(), is(false));
    }

    @Test
    public void testEmptyValueForOfficeIsInvalid() throws IOException {
        ContactCentre instance = new ObjectMapper().readValue(INVALID_ESA_LOOKUP_RETURN_EMPTY_DRS_ROUTING, ContactCentre.class);
        assertThat("empty office value is invalid", instance.isContentValid(), is(false));
    }

    @Test
    public void testNullValueForDRSRoutingIsInvalid() throws IOException {
        ContactCentre instance = new ObjectMapper().readValue(INVALID_ESA_LOOKUP_RETURN_NULL_DRS_ROUTING, ContactCentre.class);
        assertThat("null DRS routing value is invalid", instance.isContentValid(), is(false));
    }

    @Test
    public void testEmptyValueForDRSRoutingIsInvalid() throws IOException {
        ContactCentre instance = new ObjectMapper().readValue(INVALID_ESA_LOOKUP_RETURN_EMPTY_OFFICE, ContactCentre.class);
        assertThat("null DRS routing value is invalid", instance.isContentValid(), is(false));
    }

}
