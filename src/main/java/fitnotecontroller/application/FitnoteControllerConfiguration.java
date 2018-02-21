package fitnotecontroller.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

public class FitnoteControllerConfiguration extends Configuration {

    @JsonProperty("sslTruststoreFilename")
    private String sslTruststoreFilename;

    @JsonProperty("sslKeystoreFilename")
    private String sslKeystoreFilename;

    @JsonProperty("sslTruststorePassword")
    private String sslTruststorePassword;

    @JsonProperty("sslKeystorePassword")
    private String sslKeystorePassword;

    @JsonProperty("expirytimeinmilliseconds")
    private long expiryTimeInMilliSeconds;

    @JsonProperty("frequencyofexpirytimecheckinmilliseconds")
    private long frequencyOfExpiryTimeInMilliSeconds;

    @JsonProperty("ocrChecksEnabled")
    private boolean ocrChecksEnabled = true;

    @JsonProperty("forceLandscapeImageSubmission")
    private boolean landscapeImageEnforced = true;

    @NotNull
    @JsonProperty("tesseractFolderPath")
    private String tesseractFolderPath;

    @JsonProperty("easPostCodeLookUpUrl")
    private String easPostCodeLookUpUrl;

    @JsonProperty("sslTruststoreFilenameESA")
    private String sslTruststoreFilenameESA;

    @JsonProperty("sslTruststorePasswordESA")
    private String sslTruststorePasswordESA;

    @JsonProperty("esaDefaultRoutingForDRS")
    private String esaDefaultRoutingForDRS;

    @JsonProperty("rejectingOversizeImages")
    private boolean rejectingOversizeImages = true;

    @JsonProperty("pdfScanDPI")
    private int pdfScanDPI = 300;

    @JsonProperty("targetImageSizeKB")
    private int targetImageSizeKB = 500;

    @JsonProperty("greyScale")
    private boolean greyScale = true;

    @JsonProperty("maxLogChars")
    private int maxLogChars = 50;

    @JsonProperty("targetBrightness")
    private int targetBrightness = 179;

    @JsonProperty("borderLossPercentage")
    private int borderLossPercentage = 10;

    @JsonProperty("scanTargetImageSize")
    private int scanTargetImageSizeKb = 1000;

    @JsonProperty("highTarget")
    private int highTarget = 100;

    @JsonProperty("diagonalTarget")
    private int diagonalTarget = 20;

    @JsonProperty("contrastCutOff")
    private int contrastCutOff = 105;

    @NotNull
    @JsonProperty("topLeftText")
    private List<String> topLeftText;

    @NotNull
    @JsonProperty("topRightText")
    private List<String> topRightText;

    @NotNull
    @JsonProperty("baseLeftText")
    private List<String> baseLeftText;

    @NotNull
    @JsonProperty("baseRightText")
    private List<String> baseRightText;

    @JsonProperty("estimatedRequestMemoryMb")
    private int estimatedRequestMemoryMb = 25;

    @NotNull
    @JsonProperty("rabbitMqURI")
    private URI rabbitMqURI;

    @NotNull
    @JsonProperty("eventRoutingKey")
    private String eventRoutingKey;

    @NotNull
    @JsonProperty("rabbitExchangeName")
    private String rabbitExchangeName;

    public long getExpiryTimeInMilliSeconds() {
        return expiryTimeInMilliSeconds;
    }

    public long getFrequencyOfExpiryTimeInMilliSeconds() {
        return frequencyOfExpiryTimeInMilliSeconds;
    }

    public boolean isOcrChecksEnabled() {
        return ocrChecksEnabled;
    }

    public String getTesseractFolderPath() {
        return tesseractFolderPath;
    }

    public boolean isLandscapeImageEnforced() {
        return landscapeImageEnforced;
    }

    public String getSslTruststoreFilename() {
        return sslTruststoreFilename;
    }

    public String getSslKeystoreFilename() {
        return sslKeystoreFilename;
    }

    public String getSslTruststorePassword() {
        return sslTruststorePassword;
    }

    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    public String getEasPostCodeLookUpUrl() {
        return easPostCodeLookUpUrl;
    }

    public String getSslTruststoreFilenameESA() {
        return sslTruststoreFilenameESA;
    }

    public String getSslTruststorePasswordESA() {
        return sslTruststorePasswordESA;
    }

    public String getEsaDefaultRoutingForDRS() {
        return esaDefaultRoutingForDRS;
    }

    public boolean isRejectingOversizeImages() {
        return rejectingOversizeImages;
    }

    public int getTargetImageSizeKB() {
        return targetImageSizeKB;
    }

    public boolean isGreyScale() {
        return greyScale;
    }

    public int getMaxLogChars() {
        return maxLogChars;
    }

    public int getBorderLossPercentage() {
        return borderLossPercentage;
    }

    public int getTargetBrightness() {
        return targetBrightness;
    }

    public int getScanTargetImageSizeKb() {
        return scanTargetImageSizeKb;
    }

    public int getHighTarget() {
        return highTarget;
    }

    public int getDiagonalTarget() {
        return diagonalTarget;
    }

    public int getContrastCutOff() {
        return contrastCutOff;
    }

    public List<String> getTopLeftText() {
        return topLeftText;
    }

    public List<String> getTopRightText() {
        return topRightText;
    }

    public List<String> getBaseLeftText() {
        return baseLeftText;
    }

    public List<String> getBaseRightText() {
        return baseRightText;
    }

    public int getEstimatedRequestMemoryMb() {
        return estimatedRequestMemoryMb;
    }

    public int getPdfScanDPI() {
        return pdfScanDPI;
    }

    public URI getRabbitMqURI() {
        return rabbitMqURI;
    }

    public String getEventRoutingKey() {
        return eventRoutingKey;
    }

    public String getRabbitExchangeName() {
        return rabbitExchangeName;
    }
}
