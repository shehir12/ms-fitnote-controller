package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class ImageHashStore {
  private String createDateTime;
  private String lastSubmitted;
  private int submissionCount;

  public ImageHashStore() {
    // for serialisation purposes
  }

  public int getSubmissionCount() {
    return submissionCount;
  }

  public void incSubmissionCount() {
    this.submissionCount++;
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

  @JsonIgnore
  public void initCreateDateTime() {
    this.createDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
  }
}
