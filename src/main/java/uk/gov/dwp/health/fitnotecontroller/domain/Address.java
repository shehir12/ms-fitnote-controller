package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Address extends SessionItem {

    @JsonProperty("houseNameOrNumber")
    private String houseNameOrNumber;

    @JsonProperty("street")
    private String street;

    @JsonProperty("city")
    private String city;

    @JsonProperty("postcode")
    private String postcode;

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getHouseNameOrNumber() {
        return houseNameOrNumber;
    }
}
