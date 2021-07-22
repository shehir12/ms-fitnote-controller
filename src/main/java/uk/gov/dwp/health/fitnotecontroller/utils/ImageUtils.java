package uk.gov.dwp.health.fitnotecontroller.utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;

public class ImageUtils {
  protected ImageUtils() {
  }

  protected static BufferedImage changeBrightness(BufferedImage src, float val) {
    RescaleOp brighterOp = new RescaleOp(val, 0, null);
    return brighterOp.filter(src, null);
  }

  public static BufferedImage formatGrayScale(BufferedImage inputImage) {
    BufferedImage image =
        new BufferedImage(
            inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    Graphics g = image.getGraphics();

    g.drawImage(inputImage, 0, 0, null);
    g.dispose();

    return image;
  }

  protected static BufferedImage normaliseBrightness(
      BufferedImage inputImage, int targetBrightness, int loseBorderPercentage) {
    int currentBrightness = gatherBrightness(inputImage, loseBorderPercentage);
    if (0 == currentBrightness) {
      currentBrightness++;
    }
    return changeBrightness(inputImage, (float) targetBrightness / currentBrightness);
  }

  /**
   * A function that generates an average of the pixel number. Written for grayscale image closer to
   * 255 = brighter. this will lose the top & bottom 10% as well as the left & right 10% so that the
   * calculation ignores erroneous borders
   *
   * @param inputImage - image to scan
   * @return average brightness of a pixel
   */
  protected static int gatherBrightness(BufferedImage inputImage, int loseBorderPercentage) {
    Raster pixelData = inputImage.getRaster();
    int count = 0;
    int pixelCount = 1;

    // generate offset to ignore edges
    int widthOffSet = (loseBorderPercentage == 0) ? 0 : pixelData.getWidth() / loseBorderPercentage;
    int heightOffSet =
        (loseBorderPercentage == 0) ? 0 : pixelData.getHeight() / loseBorderPercentage;

    // loop through remaining data to get an average - hopefully all document
    for (int x = widthOffSet; x < (pixelData.getWidth() - widthOffSet); x++) {
      for (int y = heightOffSet; y < (pixelData.getHeight() - heightOffSet); y++) {
        count += pixelData.getSample(x, y, 0);
        pixelCount++;
      }
    }
    // return an average of the pixelData
    return pixelCount == 0 ? 0 : count / pixelCount;
  }

  public static BufferedImage increaseContrast(BufferedImage inputImage, int contrastCutOff) {
    BufferedImage contrastImage = deepCopyImage(inputImage);

    Raster pixelData = contrastImage.getRaster();
    for (int x = 0; x < (pixelData.getWidth()); x++) {
      for (int y = 0; y < (pixelData.getHeight()); y++) {
        if (pixelData.getSample(x, y, 0) < contrastCutOff) {
          contrastImage.setRGB(x, y, Color.BLACK.getRGB());
        } else {
          contrastImage.setRGB(x, y, Color.WHITE.getRGB());
        }
      }
    }
    // return an average of the pixelData
    return contrastImage;
  }

  public static BufferedImage createRotatedCopy(BufferedImage img, double angleDegrees) {
    double sin = Math.abs(Math.sin(Math.toRadians(angleDegrees)));
    double cos = Math.abs(Math.cos(Math.toRadians(angleDegrees)));
    int newWidth = (int) Math.floor(img.getWidth() * cos + img.getHeight() * sin);
    int newHeight = (int) Math.floor(img.getHeight() * cos + img.getWidth() * sin);
    BufferedImage result = new BufferedImage(newWidth, newHeight, img.getType());

    Graphics2D g = result.createGraphics();
    g.setBackground(Color.WHITE);
    g.clearRect(0, 0, newWidth, newHeight);

    g.translate((newWidth - img.getWidth()) / 2, (newHeight - img.getHeight()) / 2);
    g.rotate(
        Math.toRadians(angleDegrees), (double) img.getWidth() / 2, (double) img.getHeight() / 2);
    g.drawRenderedImage(img, null);
    g.dispose();

    return result;
  }

  private static BufferedImage deepCopyImage(BufferedImage sourceImg) {
    WritableRaster raster =
        sourceImg.copyData(sourceImg.getRaster().createCompatibleWritableRaster());
    return new BufferedImage(
        sourceImg.getColorModel(), raster, sourceImg.getColorModel().isAlphaPremultiplied(), null);
  }
}
