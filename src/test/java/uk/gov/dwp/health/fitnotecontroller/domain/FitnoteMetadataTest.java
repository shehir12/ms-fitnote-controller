package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.fitnotecontroller.domain.FitnoteMetadata;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FitnoteMetadataTest {
    private static final String EXPECTED_SERIALISED_CLASS = "{\"businessUnitID\":null,\"classification\":0,\"documentType\":0,\"documentSource\":0,\"postCode\":\"LS6 4PT\",\"nino\":null,\"benefitType\":99,\"customerMobileNumber\":\"0113 25678886\"}";

    @Test
    public void classCanBeBuiltUsingSerialised_JSON() throws IOException {
        new ObjectMapper().readValue(EXPECTED_SERIALISED_CLASS, FitnoteMetadata.class);
    }

    @Test
    public void serialisationOfBuiltClassDoesNotEqual() throws JsonProcessingException {
        FitnoteMetadata instance = new FitnoteMetadata();
        instance.setCustomerMobileNumber("0113 25678886");
        instance.setPostCode("LS6 4PT");
        instance.setBenefitType(99);

        assertThat("json output should match", new ObjectMapper().writeValueAsString(instance), is(equalTo(EXPECTED_SERIALISED_CLASS)));
    }
}