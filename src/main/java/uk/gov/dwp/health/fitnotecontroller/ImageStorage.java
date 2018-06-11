package uk.gov.dwp.health.fitnotecontroller;

import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.domain.ImageHashStore;
import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageHashException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import org.slf4j.Logger;
import uk.gov.dwp.logging.DwpEncodedLogger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageStorage {
    private static final Logger LOG = DwpEncodedLogger.getLogger(ImageStorage.class.getName());
    private static final String NULL_PAYLOAD_MSG = "Null payload object rejected";
    private static final long MEGABYTE = 1024L * 1024L;

    private Map<String, ImageHashStore> imageHashStack = new ConcurrentHashMap<>();
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
                LOG.debug("Session id does not exist, created entry for {}", sessionId);
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

    public void updateImageHashStore(String sourceImage) throws ImageHashException {
        if (sourceImage == null) {
            throw new ImageHashException("Null sourceImage rejected");
        }
        if (configuration.getImageHashSalt() == null) {
            throw new ImageHashException("Salt cannot be null");
        }

        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            hash.update(configuration.getImageHashSalt().getBytes());

            String hashImage = new String(hash.digest(sourceImage.getBytes()));

            ImageHashStore hashStoreItem = imageHashStack.get(hashImage);
            if (hashStoreItem == null) {
                LOG.debug("new image received, hash value stored to expire in {} milliseconds", configuration.getImageReplayExpiryMilliseconds());
                hashStoreItem = new ImageHashStore(getCurrentTimeMillis() + configuration.getImageReplayExpiryMilliseconds());
                imageHashStack.put(hashImage, hashStoreItem);
            }

            hashStoreItem.updateLastSubmitted();
            hashStoreItem.incSubmissionCount();

            if (hashStoreItem.getSubmissionCount() > configuration.getMaxAllowedImageReplay()) {
                LOG.info("Image hash has been replayed {} times since creation ({}).  Potential DDOS replay, aborting submission", hashStoreItem.getSubmissionCount(), hashStoreItem.getCreateDateTime());
                throw new ImageHashException("Image replay limited exceeded, rejecting");
            }

        } catch (NoSuchAlgorithmException e) {
            LOG.debug(e.getClass().getName(), e);
            LOG.error(e.getMessage());
        }
    }

    public ImagePayload updateNinoDetails(ImagePayload payload) throws ImagePayloadException {
        if (payload == null) {
            throw new ImagePayloadException(NULL_PAYLOAD_MSG);
        }

        ImagePayload payloadToChange = getPayload(payload.getSessionId());
        LOG.debug("Updating Nino for session id {}", payload.getSessionId());

        payloadToChange.setNino(payload.getNino());
        return payloadToChange;
    }

    public ImagePayload updateMobileDetails(ImagePayload payload) throws ImagePayloadException {
        if (payload == null) {
            throw new ImagePayloadException(NULL_PAYLOAD_MSG);
        }

        ImagePayload payloadToChange = getPayload(payload.getSessionId());
        LOG.debug("Updating Nino for session id {}", payload.getSessionId());

        payloadToChange.setMobileNumber(payload.getMobileNumber());
        return payloadToChange;
    }

    public void clearExpiredObjects() {
        int imageStorageBytes = 0;
        int hashStorageBytes = 0;

        for (Map.Entry<String, ImagePayload> imageObject : images.entrySet()) {
            if (images.get(imageObject.getValue().getSessionId()).getImage() != null) {
                imageStorageBytes += images.get(imageObject.getValue().getSessionId()).getRawImageSize();
            }

            long sessionExpiryTime = images.get(imageObject.getValue().getSessionId()).getExpiryTime();

            if (sessionExpiryTime < System.currentTimeMillis()) {
                LOG.info("Clearing expired session {}", imageObject.getValue().getSessionId());
                clearSession(imageObject.getValue().getSessionId());
            }
        }
        for (Map.Entry<String, ImageHashStore> imageHashObject : imageHashStack.entrySet()) {
            hashStorageBytes += imageHashObject.getKey().getBytes().length;

            if (imageHashObject.getValue().getExpiryTime() < System.currentTimeMillis()) {
                LOG.info("Clearing expired hashed image created @ {}", imageHashObject.getValue().getCreateDateTime());
                imageHashStack.remove(imageHashObject.getKey());
            }
        }

        LOG.info("STATS :: image storage {} item using {} bytes, image hash storage {} items using {} bytes", images.size(), imageStorageBytes, imageHashStack.size(), hashStorageBytes);
    }

    public void logStorageStatistics() {
        LOG.info("Total existing ImagePayload sessions = {}, ImageHashStore sessions = {}", images.size(), imageHashStack.size());
    }

    public void clearSession(String sessionId) {
        images.remove(sessionId);
    }

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}
