package uk.gov.dwp.health.fitnotecontroller.domain;

import uk.gov.dwp.health.fitnotecontroller.domain.BarcodeContents;
import uk.gov.dwp.health.fitnotecontroller.exception.QRExtractionException;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class BarcodeContentsTest {

    private static String FULL_QR_BAD_DATE_CODE = "21012016|2101216|Mr|Mickey||Mouse-TestPatient|1 Ouchthorpe La|WF1 3SP|01011990|Stress|1|0|0|0|0|0|14|21012016|04022016|0|BABD5A50-C026-11E5-A|0|The Grove Surgery|Thornhill Stree|WF1 1PG|01924784101";
    private static String FULL_QR_BAD_BOOL_CODE = "21012016|21012016|Mr|Mickey||Mouse-TestPatient|1 Ouchthorpe La|WF1 3SP|01011990|Stress|2|0|0|0|0|0|14|21012016|04022016|0|BABD5A50-C026-11E5-A|0|The Grove Surgery|Thornhill Stree|WF1 1PG|01924784101";
    private static String FULL_QR_NULL_BOOL_CODE = "21012016|21012016|Mr|Mickey||Mouse-TestPatient|1 Ouchthorpe La|WF1 3SP|01011990|Stress||0|0|0|0|0|14|21012016|04022016|0|BABD5A50-C026-11E5-A|0|The Grove Surgery|Thornhill Stree|WF1 1PG|01924784101";
    private static String FULL_QR_CODE = "21012016|21012016|Mr|Mickey||Mouse-TestPatient|1 Ouchthorpe La|WF1 3SP|01011990|Stress|1|0|0|0|0|0|14|21012016|04022016|0|BABD5A50-C026-11E5-A|0|The Grove Surgery|Thornhill Stree|WF1 1PG|01924784101";

    @Test
    public void testNullString() {
        try {
            new BarcodeContents(null);
            fail("a null qr code should always fail");

        } catch (QRExtractionException e) {
            assertThat("error should report null", e.getMessage().contains("Null string"), is(true));
        }
    }

    @Test
    public void testShortString() {
        try {
            new BarcodeContents("not|alot");
            fail("a short qr code should always fail");

        } catch (QRExtractionException e) {
            assertThat("error should report missing elements", e.getMessage().contains("Missing elements"), is(true));
        }
    }

    @Test
    public void goodDataBadDate() {
        try {
            new BarcodeContents(FULL_QR_BAD_DATE_CODE);
            fail("a full qr code with bad dates should always fail");

        } catch (QRExtractionException e) {
            assertThat("error should be for malformed date", e.getMessage().contains("malformed date"), is(true));
        }
    }

    @Test
    public void goodDataBadBool() {
        try {
            new BarcodeContents(FULL_QR_BAD_BOOL_CODE);
            fail("a full qr code with bad booleans should always fail");

        } catch (QRExtractionException e) {
            assertThat("error should be an invalid boolean", e.getMessage().contains("invalid boolean value"), is(true));
        }
    }

    @Test
    public void goodDataNullBool() {
        try {
            new BarcodeContents(FULL_QR_NULL_BOOL_CODE);
            fail("a full qr code with null booleans should always fail");

        } catch (QRExtractionException e) {
            System.out.println();
            assertThat("error should be an invalid cast", e.getMessage().contains("cast exception"), is(true));
        }
    }

    @Test
    public void goodDataTest() throws ParseException, QRExtractionException {
        Date dob = new SimpleDateFormat("ddmmyyyy").parse("01011990");
        String testSurname = "Mouse-TestPatient";
        String surgeryNumber = "01924784101";
        boolean unfitForWork = true;

        BarcodeContents qrClass = new BarcodeContents(FULL_QR_CODE);

        assertThat("the surgery phone number should match", qrClass.getSurgeryPhoneNumber(), is(surgeryNumber));
        assertThat("the fitness for work boolean should match", qrClass.isUnfitFitForWork(), is(unfitForWork));
        assertThat("the date of birth should match", qrClass.getPatientDOB().compareTo(dob), is(0));
        assertThat("the names should match", qrClass.getPatientSurname(), is(testSurname));
    }
}