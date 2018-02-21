package fitnotecontroller.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ImagePayloadTest {

    @Test
    public void validateNinoIsCorrectlySerialised() throws Exception {
        String expectedResult = "{\"ninoBody\":\"AA370773\",\"ninoSuffix\":\"A\"}";
        ImagePayload instance = new ImagePayload();
        instance.setNino("AA370773A");

        assertThat(new ObjectMapper().writeValueAsString(instance.getNinoObject()), is(equalTo(expectedResult)));
    }
}