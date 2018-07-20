package uk.gov.dwp.health.fitnotecontroller.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import uk.gov.dwp.crypto.SecureStrings;
import uk.gov.dwp.health.crypto.CryptoConfig;

import javax.crypto.SealedObject;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

public class FitnoteControllerConfiguration extends Configuration {
    private SecureStrings cipher = new SecureStrings();

    @JsonProperty("rabbitMqTruststoreFile")
    private String rabbitMqTruststoreFile;

    @JsonProperty("rabbitMqKeystoreFile")
    private String rabbitMqKeystoreFile;

    @JsonProperty("rabbitMqTruststorePass")
    private SealedObject rabbitMqTruststorePass;

    @JsonProperty("rabbitMqKeystorePass")
    private SealedObject rabbitMqKeystorePass;

    @NotNull
    @JsonProperty("sessionExpiryTimeInSeconds")
    private long sessionExpiryTimeInSeconds;

    @NotNull
    @JsonProperty("imageReplayExpirySeconds")
    private long imageReplayExpirySeconds;

    @JsonProperty("maxAllowedImageReplay")
    private int maxAllowedImageReplay = 10;

    @NotNull
    @JsonProperty("imageHashSalt")
    private SealedObject imageHashSalt;

    @NotNull
    @JsonProperty("redisStoreURI")
    private String redisStoreURI;

    @JsonProperty("ocrChecksEnabled")
    private boolean ocrChecksEnabled = true;

    @JsonProperty("forceLandscapeImageSubmission")
    private boolean landscapeImageEnforced = true;

    @NotNull
    @JsonProperty("tesseractFolderPath")
    private String tesseractFolderPath;

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
    @JsonProperty("rabbitEventRoutingKey")
    private String rabbitEventRoutingKey;

    @NotNull
    @JsonProperty("rabbitExchangeName")
    private String rabbitExchangeName;

    @NotNull
    @JsonProperty("rabbitEncryptMessages")
    private boolean rabbitEncryptMessages;

    @NotNull
    @JsonProperty("redisEncryptMessages")
    private boolean redisEncryptMessages;

    @JsonProperty("rabbitKmsCryptoConfiguration")
    private CryptoConfig rabbitKmsCryptoConfiguration;

    @JsonProperty("redisKmsCryptoConfiguration")
    private CryptoConfig redisKmsCryptoConfiguration;

    public boolean isOcrChecksEnabled() {
        return ocrChecksEnabled;
    }

    public String getTesseractFolderPath() {
        return tesseractFolderPath;
    }

    public boolean isLandscapeImageEnforced() {
        return landscapeImageEnforced;
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

    public String getRabbitEventRoutingKey() {
        return rabbitEventRoutingKey;
    }

    public String getRabbitExchangeName() {
        return rabbitExchangeName;
    }

    public boolean isRabbitEncryptMessages() {
        return rabbitEncryptMessages;
    }

    public CryptoConfig getRabbitKmsCryptoConfiguration() {
        return rabbitKmsCryptoConfiguration;
    }

    public String getRabbitMqTruststoreFile() {
        return rabbitMqTruststoreFile;
    }

    public String getRabbitMqKeystoreFile() {
        return rabbitMqKeystoreFile;
    }

    public String getRabbitMqTruststorePass() {
        return cipher.revealString(rabbitMqTruststorePass);
    }

    public String getRabbitMqKeystorePass() {
        return cipher.revealString(rabbitMqKeystorePass);
    }

    public void setRabbitMqTruststorePass(String rabbitMqTruststorePass) {
        this.rabbitMqTruststorePass = cipher.sealString(rabbitMqTruststorePass);
    }

    public void setRabbitMqKeystorePass(String rabbitMqKeystorePass) {
        this.rabbitMqKeystorePass = cipher.sealString(rabbitMqKeystorePass);
    }

    public int getMaxAllowedImageReplay() {
        return maxAllowedImageReplay;
    }

    public String getImageHashSalt() {
        return cipher.revealString(imageHashSalt);
    }

    public void setImageHashSalt(String imageHashSalt) {
        this.imageHashSalt = cipher.sealString(imageHashSalt);
    }

    public String getRedisStoreURI() {
        return redisStoreURI;
    }

    public CryptoConfig getRedisKmsCryptoConfiguration() {
        return redisKmsCryptoConfiguration;
    }

    public boolean isRedisEncryptMessages() {
        return redisEncryptMessages;
    }

    public long getSessionExpiryTimeInSeconds() {
        return sessionExpiryTimeInSeconds;
    }

    public long getImageReplayExpirySeconds() {
        return imageReplayExpirySeconds;
    }
}
