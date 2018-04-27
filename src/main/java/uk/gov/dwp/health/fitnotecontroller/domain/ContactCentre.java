package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContactCentre {

    @JsonProperty("office")
    private String office;

    @JsonProperty("mailsite")
    private String mailsite;

    @JsonProperty("postcode")
    private String postcode;

    @JsonProperty("benefitCentre")
    private String benefitCentre;

    @JsonProperty("freepostAddress")
    private String freepostAddress;

    @JsonProperty("officeRoutingCodeForDRS")
    private String officeRoutingCodeForDRS;

    public String getOffice() {
        return office;
    }

    public String getMailsite() {
        return mailsite;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getBenefitCentre() {
        return benefitCentre;
    }

    public String getFreepostAddress() {
        return freepostAddress;
    }

    public String getOfficeRoutingCodeForDRS() {
        return officeRoutingCodeForDRS;
    }

    public boolean isContentValid() {
        boolean isValid = !((null == getOffice()) || (getOffice().trim().isEmpty()));
        if (isValid) {
            isValid = !((null == getOfficeRoutingCodeForDRS()) || (getOfficeRoutingCodeForDRS().trim().isEmpty()));
        }
        return isValid;
    }
}
