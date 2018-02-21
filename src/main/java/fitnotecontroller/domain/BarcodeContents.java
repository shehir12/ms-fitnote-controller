package fitnotecontroller.domain;

import fitnotecontroller.exception.QRExtractionException;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BarcodeContents {
    private static final Logger LOG = DwpEncodedLogger.getLogger(BarcodeContents.class.getName());
    private Date caseAssessmentDate;
    private Date statementDate;

    private String patientTitle;
    private String patientFirstname;
    private String patientSecondName;
    private String patientSurname;
    private String patientAddress;
    private String patientPostcode;
    private Date patientDOB;
    private String condition;

    private boolean unfitFitForWork;
    private boolean fitForWorkWithConditions;
    private boolean phasedReturnToWork;
    private boolean alteredHours;
    private boolean amendedDuties;
    private boolean workplaceAdaptions;
    private String fitnoteDuration;
    private Date fitnoteStartDate;
    private Date fitnoteExpiryDate;
    private boolean followUpRequired;

    private String med3Identity;
    private boolean duplicateCopy;

    private String surgeryName;
    private String surgeryAddress;
    private String surgeryPostcode;
    private String surgeryPhoneNumber;

    public BarcodeContents(String qrCodeContents) throws QRExtractionException {
        if (null == qrCodeContents) {
            throw new QRExtractionException("Null string.  The QR code is null and cannot be extracted");
        }

        String[] qrTokens = qrCodeContents.split("\\|");
        if (qrTokens.length < 26) {
            throw new QRExtractionException(String.format("Missing elements.  The QR code should have 26 elements, only %d detected", qrTokens.length));
        }

        try {
            this.caseAssessmentDate = buildDate(qrTokens[0]);
            this.statementDate = buildDate(qrTokens[1]);
            this.patientTitle = qrTokens[2];
            this.patientFirstname = qrTokens[3];
            this.patientSecondName = qrTokens[4];
            this.patientSurname = qrTokens[5];
            this.patientAddress = qrTokens[6];
            this.patientPostcode = qrTokens[7];
            this.patientDOB = buildDate(qrTokens[8]);
            this.condition = qrTokens[9];

            this.unfitFitForWork = buildBoolean(qrTokens[10]);
            this.fitForWorkWithConditions = buildBoolean(qrTokens[11]);
            this.phasedReturnToWork = buildBoolean(qrTokens[12]);
            this.alteredHours = buildBoolean(qrTokens[13]);
            this.amendedDuties = buildBoolean(qrTokens[14]);
            this.workplaceAdaptions = buildBoolean(qrTokens[15]);
            this.fitnoteDuration = qrTokens[16];
            this.fitnoteStartDate = buildDate(qrTokens[17]);
            this.fitnoteExpiryDate = buildDate(qrTokens[18]);
            this.followUpRequired = buildBoolean(qrTokens[19]);

            this.med3Identity = qrTokens[20];
            this.duplicateCopy = buildBoolean(qrTokens[21]);

            this.surgeryName = qrTokens[22];
            this.surgeryAddress = qrTokens[23];
            this.surgeryPostcode = qrTokens[24];
            this.surgeryPhoneNumber = qrTokens[25];

        } catch (ParseException e) {
            throw new QRExtractionException(String.format("Build date error : %s", e.getMessage()), e);
        } catch (NumberFormatException e) {
            throw new QRExtractionException(String.format("Boolean cast exception : %s", e.getMessage()), e);
        }

        // qr code success
        LOG.info(String.format("SUCCESSFULLY decoded the QR code with %d elements", qrTokens.length));
    }

    private Date buildDate(String dateString) throws ParseException, QRExtractionException {
        if ((dateString == null) || (dateString.trim().length() < 8)) {
            throw new QRExtractionException("null or malformed date string");
        }
        SimpleDateFormat formatter = new SimpleDateFormat("ddmmyyyy");
        return formatter.parse(dateString.toUpperCase().trim());
    }

    private boolean buildBoolean(String boolString) throws QRExtractionException {
        if ((boolString == null) || (Integer.parseInt(boolString) > 1) || (Integer.parseInt(boolString) < 0)) {
            throw new QRExtractionException(String.format("%s is null or an invalid boolean value", boolString));
        }
        int boolValue = Integer.parseInt(boolString);
        return boolValue == 1;
    }

    public Date getCaseAssessmentDate() {
        return caseAssessmentDate;
    }

    public Date getStatementDate() {
        return statementDate;
    }

    public String getPatientTitle() {
        return patientTitle;
    }

    public String getPatientFirstname() {
        return patientFirstname;
    }

    public String getPatientSurname() {
        return patientSurname;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public String getPatientPostcode() {
        return patientPostcode;
    }

    public Date getPatientDOB() {
        return patientDOB;
    }

    public String getCondition() {
        return condition;
    }

    public boolean isUnfitFitForWork() {
        return unfitFitForWork;
    }

    public boolean isFitForWorkWithConditions() {
        return fitForWorkWithConditions;
    }

    public boolean isPhasedReturnToWork() {
        return phasedReturnToWork;
    }

    public boolean isAlteredHours() {
        return alteredHours;
    }

    public boolean isAmendedDuties() {
        return amendedDuties;
    }

    public boolean isWorkplaceAdaptions() {
        return workplaceAdaptions;
    }

    public String getFitnoteDuration() {
        return fitnoteDuration;
    }

    public Date getFitnoteStartDate() {
        return fitnoteStartDate;
    }

    public Date getFitnoteExpiryDate() {
        return fitnoteExpiryDate;
    }

    public boolean isFollowUpRequired() {
        return followUpRequired;
    }

    public String getSurgeryName() {
        return surgeryName;
    }

    public String getSurgeryAddress() {
        return surgeryAddress;
    }

    public String getSurgeryPostcode() {
        return surgeryPostcode;
    }

    public String getSurgeryPhoneNumber() {
        return surgeryPhoneNumber;
    }

    public String getPatientSecondName() {
        return patientSecondName;
    }

    public String getMed3Identity() {
        return med3Identity;
    }

    public boolean isDuplicateCopy() {
        return duplicateCopy;
    }
}
