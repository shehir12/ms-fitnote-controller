package uk.gov.dwp.health.fitnotecontroller.domain;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class ImageHashStore {
    private String createDateTime;
    private String lastSubmitted;
    private int submissionCount;
    private long expiryTime;

    public ImageHashStore(long expiryTime) {
        this.createDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        setExpiryTime(expiryTime);
    }

    public int getSubmissionCount() {
        return submissionCount;
    }

    public void incSubmissionCount() {
        this.submissionCount++;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public String getLastSubmitted() {
        return lastSubmitted;
    }

    public void updateLastSubmitted() {
        this.lastSubmitted = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    public String getCreateDateTime() {
        return createDateTime;
    }
}
