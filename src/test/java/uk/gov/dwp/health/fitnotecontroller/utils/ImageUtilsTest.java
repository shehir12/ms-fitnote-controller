package uk.gov.dwp.health.fitnotecontroller.utils;

import uk.gov.dwp.health.fitnotecontroller.domain.ImagePayload;
import gherkin.deps.net.iharder.Base64;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ImageUtilsTest extends ImageUtils {
    private static final String IMAGE_FILE = "/DarkPage.jpg";
    private BufferedImage localImage;

    private BufferedImage getTestImage() throws IOException {
        String imageString = Base64.encodeFromFile(this.getClass().getResource(IMAGE_FILE).getPath());
        ImagePayload payload = new ImagePayload();
        payload.setImage(imageString);
        byte[] decode = org.apache.commons.codec.binary.Base64.decodeBase64(payload.getImage());
        ByteArrayInputStream inputStream = new ByteArrayInputStream(decode);
        return ImageIO.read(inputStream);
    }

    @Before
    public void setUp() throws IOException {
        localImage = getTestImage();
    }

    @Test
    public void validateChangeBrightness() {
        int brightness = gatherBrightness(localImage, 0);
        BufferedImage alteredLocalImage = changeBrightness(localImage, 1.1f);
        int brighter = gatherBrightness(alteredLocalImage, 0);
        assertTrue(String.format("%d < %d", brightness, brighter), brightness < brighter);
    }

    @Test
    public void validateNoExceptionWhenChangeBrightnessWithNegativeValue() {
        int origBrightness = gatherBrightness(localImage, 0);

        BufferedImage updatedImage = changeBrightness(localImage, -10);
        int newBrightness = gatherBrightness(updatedImage, 0);

        assertTrue(origBrightness > newBrightness);
    }

    @Test
    public void validateNormaliseBrightness() {
        int brightness = gatherBrightness(localImage, 0);
        BufferedImage alteredLocalImage = normaliseBrightness(localImage, brightness + 10, 0);//
        assertTrue(brightness < gatherBrightness(alteredLocalImage, 0));
    }

    @Test
    public void validateGatherBrightness() {
        int brightness = gatherBrightness(localImage, 0);
        assertTrue(brightness > 0);
        assertTrue(256 > brightness);
    }

    @Test
    public void validateFormatGrayScale() {
        BufferedImage alteredLocalImage = formatGrayScale(localImage);
        assertTrue(localImage.getColorModel().getColorSpace().isCS_sRGB());
        assertFalse(alteredLocalImage.getColorModel().getColorSpace().isCS_sRGB());
    }
}