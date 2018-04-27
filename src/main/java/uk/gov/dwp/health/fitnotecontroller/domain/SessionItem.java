package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionItem {

    @JsonProperty("sessionId")
    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }
}
