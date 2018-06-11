package uk.gov.dwp.health.fitnotecontroller.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import uk.gov.dwp.logging.DwpEncodedLogger;
import uk.gov.dwp.regex.NinoValidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ImagePayload {
    private static final Logger LOG = DwpEncodedLogger.getLogger(ImagePayload.class.getName());

    public enum Status {
        CREATED,
        UPLOADED,
        CHECKING,
        FAILED_IMG_SIZE,
        PASS_IMG_SIZE,
        FAILED_IMG_OCR,
        FAILED_IMG_OCR_PARTIAL,
        PASS_IMG_OCR,
        FAILED_IMG_BARCODE,
        PASS_IMG_BARCODE,
        SUCCEEDED,
        FAILED_ERROR
    }

    @JsonView(Views.SessionOnly.class)
    private String sessionId;

    private byte[] image;
    private long expiryTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nino;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String mobileNumber;

    private Status fitnoteCheckStatus;
    private Status barcodeCheckStatus;
    private byte[] barcodeImage;
    private BarcodeContents barcodeContents;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Address claimantAddress;

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public NinoValidator getNinoObject() {
        return (nino.length() == 9) ? new NinoValidator(nino.substring(0, 8), nino.substring(8)) : new NinoValidator(nino, "");
    }

    @JsonIgnore
    public int getRawImageSize() {
        return this.image != null ? this.image.length : 0;
    }

    public String getNino() {
        return nino;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setNino(String nino) {
        this.nino = nino;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getImage() {
        return uncompress(image);
    }

    public void setImage(String image) {
        this.image = compress(image);
    }

    public String getSessionId() {
        return sessionId;
    }

    public Status getBarcodeCheckStatus() {
        return barcodeCheckStatus;
    }

    public void setBarcodeCheckStatus(Status barcodeCheckStatus) {
        this.barcodeCheckStatus = barcodeCheckStatus;
    }

    public void setFitnoteCheckStatus(Status fitnoteCheckStatus) {
        this.fitnoteCheckStatus = fitnoteCheckStatus;
    }

    public Status getFitnoteCheckStatus() {
        return fitnoteCheckStatus;
    }

    public Address getClaimantAddress() {
        return claimantAddress;
    }

    public void setClaimantAddress(Address claimantAddress) {
        this.claimantAddress = claimantAddress;
    }

    public String getBarcodeImage() {
        return uncompress(barcodeImage);
    }

    public void setBarcodeImage(String barcodeImage) {
        this.barcodeImage = compress(barcodeImage);
    }

    public BarcodeContents getBarcodeContents() {
        return barcodeContents;
    }

    public void setBarcodeContents(BarcodeContents barcodeContents) {
        this.barcodeContents = barcodeContents;
    }

    private byte[] compress(String str) {
        if (str != null) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(out);
                gzip.write(str.getBytes());
                gzip.close();

                return out.toByteArray();

            } catch (IOException e) {
                LOG.error("Error compressing string : {}", e.getMessage());
                LOG.debug(e.getClass().getName(), e);
            }
        }

        return null; //NOSONAR - we want to return null
    }

    private String uncompress(byte[] str) {
        if (str != null) {
            GZIPInputStream gzipIn = null;
            try {
                gzipIn = new GZIPInputStream(new ByteArrayInputStream(str));
                return IOUtils.toString(gzipIn);
            } catch (IOException e) {
                LOG.error("Error uncompressing string : {}", e.getMessage());
                LOG.debug(e.getClass().getName(), e);
            }
        }
        return null;
    }
}
