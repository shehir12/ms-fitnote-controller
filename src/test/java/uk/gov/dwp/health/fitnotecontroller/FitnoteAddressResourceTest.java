package uk.gov.dwp.health.fitnotecontroller;


import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.fitnotecontroller.FitnoteAddressResource;
import uk.gov.dwp.health.fitnotecontroller.ImageStorage;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.Address;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.exception.NewAddressException;
import uk.gov.dwp.health.fitnotecontroller.utils.JsonValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FitnoteAddressResourceTest {

    @Mock
    private JsonValidator jsonValidator;

    @Mock
    private FitnoteControllerConfiguration config;

    private FitnoteAddressResource resourceUnderTest;
    private ImageStorage imageStore;

    private final String VALID_ADDRESS = "{ \"sessionId\" :\"123456\", \"houseNameOrNumber\" : \"254\", \"street\" : \"Bakers Street\", \"city\": \"London\", \"postcode\" : \"NE12 9LG\"}";
    private final String INVALID_ADDRESS = "{}";

    @Before
    public void setup() throws ImagePayloadException {
        when(config.getExpiryTimeInMilliSeconds()).thenReturn(new Long(1000));

        imageStore = new ImageStorage(config);
        imageStore.getPayload("123456");

        resourceUnderTest = new FitnoteAddressResource(imageStore, jsonValidator);
    }

    @Test
    public void newAddressReturns500WhenUnexpectedExceptionOccurs() {
        Response response = resourceUnderTest.updateAddress(INVALID_ADDRESS);
        assertThat(response.getStatus(), is(500));

    }

    @Test
    public void newAddressReturns400WhenInvalidJsonBodyIsSupplied() throws NewAddressException {
        when(jsonValidator.validateAndTranslateAddress(INVALID_ADDRESS)).thenThrow(NewAddressException.class);
        Response response = resourceUnderTest.updateAddress(INVALID_ADDRESS);
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void newAddressReturns200WhenAValidJsonBodyIsSupplied() throws IOException, NewAddressException {
        Address mockNewAddress = new ObjectMapper().readValue(VALID_ADDRESS, Address.class);
        when(jsonValidator.validateAndTranslateAddress(VALID_ADDRESS)).thenReturn(mockNewAddress);
        Response response = resourceUnderTest.updateAddress(VALID_ADDRESS);
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void imagePayloadIsUpdatedWithNewAddressWhenSessionIDExists() throws NewAddressException, IOException, ImagePayloadException {
        Address mockNewAddress = new ObjectMapper().readValue(VALID_ADDRESS, Address.class);
        when(jsonValidator.validateAndTranslateAddress(VALID_ADDRESS)).thenReturn(mockNewAddress);
        Response response = resourceUnderTest.updateAddress(VALID_ADDRESS);
        Address newAddress = imageStore.getPayload("123456").getClaimantAddress();
        assertThat(newAddress.getHouseNameOrNumber(), is("254"));
        assertThat(newAddress.getStreet(), is("Bakers Street"));
        assertThat(newAddress.getCity(), is("London"));
        assertThat(newAddress.getPostcode(), is("NE12 9LG"));
    }
}
