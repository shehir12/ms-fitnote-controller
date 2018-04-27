package uk.gov.dwp.health.fitnotecontroller;

import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageHashException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import org.slf4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.dwp.logging.DwpEncodedLogger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("squid:S2925") // for Thread.sleep()
@RunWith(MockitoJUnitRunner.class)
public class ImageStorageTest {
    private final static Logger LOGGER = DwpEncodedLogger.getLogger(ImageStorageTest.class.getName());
    private final static long SYSTEM_TIME_FAKE_VALUE = 0;

    @Mock
    private FitnoteControllerConfiguration configuration;

    private ImageStorage persister;

    @Before
    public void setup() {
        when(configuration.getExpiryTimeInMilliSeconds()).thenReturn(new Long(500));
        when(configuration.getMaxAllowedImageReplay()).thenReturn(2);
        when(configuration.getImageHashSalt()).thenReturn("salt");
        persister = new ImageStorage(configuration) {
            @Override
            protected long getCurrentTimeMillis() {
                return SYSTEM_TIME_FAKE_VALUE;
            }
        };
    }

    @Test
    public void imageCanBePersistedAndRetrieved() throws ImagePayloadException {
        String sessionId = "123456";
        persister.getPayload(sessionId);

        assertThat(persister.getPayload(sessionId).getExpiryTime(), is(equalTo(configuration.getExpiryTimeInMilliSeconds())));
        assertThat(persister.getPayload(sessionId).getSessionId(), is(equalTo((sessionId))));
    }

    @Test
    public void imageCanBePersistedAndRetrievedMultipleTimes() throws ImagePayloadException {
        String sessionId = "123456";
        ImagePayload imagePayload = persister.getPayload(sessionId);

        assertThat(persister.getPayload(sessionId).getSessionId(), is(equalTo((sessionId))));
        assertThat(persister.getPayload(sessionId), is(equalTo((imagePayload))));
    }

    @Test
    public void invalidSessionIdsReturnNullImagePayload() {
        try {
            persister.getPayload(null);
        } catch (ImagePayloadException e) {
            assertThat(e.getMessage(), containsString("Null sessionId"));
        }
    }

    @Test
    public void updateNullImagePayload() {
        try {
            persister.updateNinoDetails(null);
        } catch (ImagePayloadException e) {
            assertThat(e.getMessage(), containsString("Null payload"));
        }
    }

    @Test
    public void updateUnknownPayload() throws ImagePayloadException {
        ImagePayload payload = new ImagePayload();
        payload.setSessionId("unknownsessionid");

        assertThat(persister.updateNinoDetails(payload).getSessionId(), is(equalTo(payload.getSessionId())));
    }

    @Test
    public void updateNinoToStoredImage() throws ImagePayloadException {
        String sessionId = "123456";
        persister.getPayload(sessionId);
        String nino = "AA370773A";

        ImagePayload newPayloadWithSameSessionId = new ImagePayload();
        newPayloadWithSameSessionId.setSessionId(sessionId);
        newPayloadWithSameSessionId.setNino(nino);
        ImagePayload returnedPayload = persister.updateNinoDetails(newPayloadWithSameSessionId);

        assertThat(returnedPayload.getNino(), is(equalTo(nino)));
    }

    @Test
    public void updateMobileNumberToStoredImage() throws ImagePayloadException {
        String sessionId = "123456";
        persister.getPayload(sessionId);
        String mobileNumber = "0777767676766";

        ImagePayload newPayloadWithSameSessionId = new ImagePayload();
        newPayloadWithSameSessionId.setSessionId(sessionId);
        newPayloadWithSameSessionId.setMobileNumber(mobileNumber);
        ImagePayload returnedPayload = persister.updateMobileDetails(newPayloadWithSameSessionId);

        assertThat(returnedPayload.getMobileNumber(), is(equalTo(mobileNumber)));
    }


    @Test
    public void expiredPayloadIsDeleted() throws ImagePayloadException, InterruptedException {
        String sessionId = "123456";
        String nino = "AA370773A";

        ImagePayload newPayloadWithSameSessionId = new ImagePayload();
        newPayloadWithSameSessionId.setSessionId(sessionId);
        newPayloadWithSameSessionId.setNino(nino);

        persister.updateNinoDetails(newPayloadWithSameSessionId);

        Thread.sleep(configuration.getExpiryTimeInMilliSeconds() + 500);
        persister.clearExpiredObjects();

        ImagePayload payloadItem = persister.getPayload(sessionId);
        assertThat(payloadItem.getSessionId(), is(equalTo((sessionId))));
        assertNotEquals(payloadItem.getNino(), nino);
    }

    @Test
    public void unexpiredPayloadPersists() throws ImagePayloadException {
        String sessionId = "123456";
        String nino = "AA370773A";

        ImagePayload newPayloadWithSameSessionId = new ImagePayload();
        newPayloadWithSameSessionId.setSessionId(sessionId);
        newPayloadWithSameSessionId.setNino(nino);

        persister.updateNinoDetails(newPayloadWithSameSessionId);
        persister.clearExpiredObjects();

        ImagePayload payloadItem = persister.getPayload(sessionId);
        assertThat(payloadItem.getSessionId(), is(equalTo((sessionId))));
        assertNotEquals(payloadItem.getNino(), nino);
    }

    @Test
    public void updateDetailsAddsNinoWithSuffixToStoredImage() throws ImagePayloadException {
        String sessionId = "123456";
        String nino = "AA370773A";

        ImagePayload newPayloadWithSameSessionId = new ImagePayload();
        newPayloadWithSameSessionId.setSessionId(sessionId);
        newPayloadWithSameSessionId.setNino(nino);
        ImagePayload returnedPayload = persister.updateNinoDetails(newPayloadWithSameSessionId);

        ImagePayload updatedPayload = persister.getPayload(sessionId);

        assertThat(updatedPayload.getNinoObject().getNinoBody(), is(equalTo(nino.substring(0, 8))));
        assertThat(updatedPayload.getNinoObject().getNinoSuffix(), is(equalTo(nino.substring(8))));
        assertThat(returnedPayload, is(updatedPayload));
    }

    @Test
    public void updateDetailsAddsNinoWithoutSuffixToStoredImage() throws ImagePayloadException {
        String sessionId = "123456";
        String nino = "AA370773";

        ImagePayload newPayloadWithSameSessionId = new ImagePayload();
        newPayloadWithSameSessionId.setSessionId(sessionId);
        newPayloadWithSameSessionId.setNino(nino);
        ImagePayload returnedPayload = persister.updateNinoDetails(newPayloadWithSameSessionId);

        ImagePayload updatedPayload = persister.getPayload(sessionId);

        assertThat(updatedPayload.getNinoObject().getNinoBody(), is(equalTo(nino.substring(0, 8))));
        assertThat(updatedPayload.getNinoObject().getNinoSuffix(), is(equalTo("")));
        assertThat(returnedPayload, is(updatedPayload));
    }

    @Test
    public void testThreadsCannotBreakTheImageStorageClass() throws ImagePayloadException, InterruptedException {
        String sessionId = "888888";
        persister.getPayload(sessionId);
        String mobileNumber = "12345678909";

        Thread creator = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    for (int i = 0; i < 5; i++) {
                        ImagePayload originalPayload = persister.getPayload(sessionId);
                        Thread.sleep(1000);

                        originalPayload.setMobileNumber(Long.toString(System.currentTimeMillis()));
                    }

                } catch (ImagePayloadException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread destroyer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    for (int i = 0; i < 5; i++) {
                        LOGGER.info("Clearing session {}", sessionId);
                        persister.clearSession(sessionId);
                        Thread.sleep(980);
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        creator.start();
        destroyer.start();
        creator.join();
    }

    @Test(expected = ImageHashException.class)
    public void testNullSubmissionFails() throws ImageHashException {
        persister.updateImageHashStore(null);
    }

    @Test
    public void testHashGetsExpired() throws ImageHashException, InterruptedException {
        persister.updateImageHashStore("i-am-an-image");
        persister.updateImageHashStore("i-am-an-image");

        Thread.sleep(configuration.getExpiryTimeInMilliSeconds() + 500);
        persister.clearExpiredObjects();

        persister.updateImageHashStore("i-am-an-image");
    }

    @Test(expected = ImageHashException.class)
    public void testExceptionForNullSalt() throws ImageHashException {
        when(configuration.getImageHashSalt()).thenReturn(null);
        persister.updateImageHashStore("i-am-an-iage");
    }

    @Test(expected = ImageHashException.class)
    public void testHashLimitsIsReached() throws ImageHashException {
        persister.updateImageHashStore("i-am-an-image");
        persister.updateImageHashStore("i-am-an-image");
        persister.updateImageHashStore("i-am-an-image");
    }
}