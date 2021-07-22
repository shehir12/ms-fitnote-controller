package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

public class Address extends SessionItem {

  @JsonView(Views.QueryAddressDetails.class)
  @JsonProperty("houseNameOrNumber")
  private String houseNameOrNumber;

  @JsonView(Views.QueryAddressDetails.class)
  @JsonProperty("street")
  private String street;

  @JsonView(Views.QueryAddressDetails.class)
  @JsonProperty("city")
  private String city;

  @JsonView(Views.QueryAddressDetails.class)
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
