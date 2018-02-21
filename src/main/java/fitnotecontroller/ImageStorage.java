package fitnotecontroller;

import fitnotecontroller.application.FitnoteControllerConfiguration;
import fitnotecontroller.domain.ImagePayload;
import fitnotecontroller.exception.ImagePayloadException;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageStorage {
    private static final Logger LOG = DwpEncodedLogger.getLogger(ImageStorage.class.getName());
    private static final String NULL_PAYLOAD_MSG = "Null payload object rejected";
    private Map<String, ImagePayload> images = new ConcurrentHashMap<>();
    private final FitnoteControllerConfiguration configuration;

    public ImageStorage(FitnoteControllerConfiguration configuration) {
        this.configuration = configuration;
    }

    public ImagePayload getPayload(String sessionId) throws ImagePayloadException {
        if (sessionId == null) {
            throw new ImagePayloadException("Null sessionId rejected");
        }

        synchronized (this) {
            ImagePayload payloadItem;

            if (images.containsKey(sessionId)) {
                payloadItem = images.get(sessionId);

            } else {
                LOG.debug(String.format("Session id does not exist, created entry for %s", sessionId));
                payloadItem = new ImagePayload();

                payloadItem.setExpiryTime(getCurrentTimeMillis() + configuration.getExpiryTimeInMilliSeconds());
                payloadItem.setBarcodeCheckStatus(ImagePayload.Status.CREATED);
                payloadItem.setFitnoteCheckStatus(ImagePayload.Status.CREATED);
                payloadItem.setSessionId(sessionId);
                images.put(sessionId, payloadItem);
            }

            return payloadItem;
        }
    }

    public ImagePayload updateNinoDetails(ImagePayload payload) throws ImagePayloadException {
        if (payload == null) {
            throw new ImagePayloadException(NULL_PAYLOAD_MSG);
        }

        ImagePayload payloadToChange = getPayload(payload.getSessionId());
        LOG.debug("Updating Nino for session id " + payload.getSessionId());

        payloadToChange.setNino(payload.getNino());
        return payloadToChange;
    }

    public ImagePayload updateMobileDetails(ImagePayload payload) throws ImagePayloadException {
        if (payload == null) {
            throw new ImagePayloadException(NULL_PAYLOAD_MSG);
        }

        ImagePayload payloadToChange = getPayload(payload.getSessionId());
        LOG.debug("Updating Nino for session id " + payload.getSessionId());

        payloadToChange.setMobileNumber(payload.getMobileNumber());
        return payloadToChange;
    }

    public void sessionExpired() {
        for (Map.Entry<String, ImagePayload> imageObject : images.entrySet()) {
            long sessionExpiryTime = images.get(imageObject.getValue().getSessionId()).getExpiryTime();
            if (sessionExpiryTime < System.currentTimeMillis()) {
                LOG.info(String.format("Clearing expired session %s", imageObject.getValue().getSessionId()));
                clearSession(imageObject.getValue().getSessionId());
            }
        }
    }

    public void clearSession(String sessionId) {
        images.remove(sessionId);
    }

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}
