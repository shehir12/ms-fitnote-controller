package uk.gov.dwp.health.fitnotecontroller.domain;

import uk.gov.dwp.components.drs.Metadata;
import uk.gov.dwp.regex.NinoValidator;

public class FitnoteMetadata extends Metadata {

  private String postCode;
  private NinoValidator nino;
  private int benefitType;
  private String customerMobileNumber;

  public String getCustomerMobileNumber() {
    return customerMobileNumber;
  }

  public void setCustomerMobileNumber(String customerMobileNumber) {
    this.customerMobileNumber = customerMobileNumber;
  }

  public String getPostCode() {
    return postCode;
  }

  public void setPostCode(String postCode) {
    this.postCode = postCode;
  }

  public NinoValidator getNino() {
    return nino;
  }

  public void setNino(NinoValidator nino) {
    this.nino = nino;
  }

  public int getBenefitType() {
    return benefitType;
  }

  public void setBenefitType(int benefitType) {
    this.benefitType = benefitType;
  }
}
