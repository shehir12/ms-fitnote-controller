package uk.gov.dwp.health.fitnotecontroller.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import uk.gov.dwp.crypto.SecureStrings;
import uk.gov.dwp.health.crypto.CryptoConfig;
import uk.gov.dwp.health.messageq.amazon.items.AmazonConfigBase;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class FitnoteControllerConfiguration extends Configuration {
  private SecureStrings cipher = new SecureStrings();

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

  @JsonProperty("ocrVerticalSlice")
  private int ocrVerticalSlice = 6;

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
  @JsonProperty("snsTopicName")
  private String snsTopicName;

  @NotNull
  @JsonProperty("snsRoutingKey")
  private String snsRoutingKey;

  @NotNull
  @JsonProperty("snsSubject")
  private String snsSubject;

  @JsonProperty("snsEncryptMessages")
  private boolean snsEncryptMessages = true;

  @NotNull
  @JsonProperty("snsConfiguration")
  private AmazonConfigBase snsConfiguration;

  @NotNull
  @JsonProperty("redisEncryptMessages")
  private boolean redisEncryptMessages;

  @JsonProperty("snsKmsCryptoConfiguration")
  private CryptoConfig snsKmsCryptoConfiguration;

  @JsonProperty("redisKmsCryptoConfiguration")
  private CryptoConfig redisKmsCryptoConfiguration;

  @JsonProperty("applicationInfoEnabled")
  private boolean applicationInfoEnabled;

  public FitnoteControllerConfiguration() throws NoSuchPaddingException,
      NoSuchAlgorithmException, InvalidKeyException {
    // Empty constructor
  }

  public boolean isApplicationInfoEnabled() {
    return applicationInfoEnabled;
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

  public int getMaxAllowedImageReplay() {
    return maxAllowedImageReplay;
  }

  public String getImageHashSalt() {
    return cipher.revealString(imageHashSalt);
  }

  public void setImageHashSalt(String imageHashSalt) throws IOException, IllegalBlockSizeException {
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

  public String getSnsTopicName() {
    return snsTopicName;
  }

  public String getSnsRoutingKey() {
    return snsRoutingKey;
  }

  public String getSnsSubject() {
    return snsSubject;
  }

  public boolean isSnsEncryptMessages() {
    return snsEncryptMessages;
  }

  public AmazonConfigBase getSnsConfiguration() {
    return snsConfiguration;
  }

  public CryptoConfig getSnsKmsCryptoConfiguration() {
    return snsKmsCryptoConfiguration;
  }

  public int getOcrVerticalSlice() {
    return ocrVerticalSlice;
  }
}
