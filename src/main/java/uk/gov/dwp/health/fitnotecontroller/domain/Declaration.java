package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Declaration extends SessionItem {

    @JsonProperty("accepted")
    private Boolean accepted;

    public Boolean isAccepted() {
        return accepted;
    }
}
