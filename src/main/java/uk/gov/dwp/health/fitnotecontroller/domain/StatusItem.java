package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatusItem {

  @JsonProperty("fitnoteStatus")
  private String fitnoteStatus;

  public String getFitnoteStatus() {
    return fitnoteStatus;
  }

}
