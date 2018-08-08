package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

public class SessionItem {

    @JsonView(Views.QueryAddressDetails.class)
    @JsonProperty("sessionId")
    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }
}
