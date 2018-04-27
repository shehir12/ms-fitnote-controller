package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatusItem {

    @JsonProperty("fitnoteStatus")
    private String fitnoteStatus;

    @JsonProperty("barcodeStatus")
    private String barcodeStatus;

    public String getFitnoteStatus() {
        return fitnoteStatus;
    }

    public String getBarcodeStatus() {
        return barcodeStatus;
    }

}
