package fitnotecontroller.domain;

import gov.dwp.securecomms.drs.Metadata;
import gov.dwp.utilities.formatvalidation.NinoValidator;

public class FitnoteMetadata extends Metadata {

    private String postCode;
    private String officePostcode;
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

    public String getOfficePostcode() {
        return officePostcode;
    }

    public void setOfficePostcode(String officePostcode) {
        this.officePostcode = officePostcode;
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
