package uk.gov.dwp.health.fitnotecontroller.utils;

import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImageCompressorTest {
    private static final int TARGET_IMAGE_SIZE = 500;
    private static BufferedImage baseImageNormal;
    private static byte[] standardPdfDoc;

    @Mock
    private FitnoteControllerConfiguration config;

    @BeforeClass
    public static void init() throws IOException {
        standardPdfDoc = FileUtils.readFileToByteArray(new File("src/test/resources/FullPage_Portrait.pdf"));
        baseImageNormal = ImageIO.read(new File("src/test/resources/FullPage_Portrait.jpg"));
    }

    @Test
    public void testWithGreyScaleNormal() throws ImageCompressException, IOException {
        writeToFile("CompressorTest_GS.jpg", true);
    }

    @Test
    public void testWithColourNormal() throws ImageCompressException, IOException {
        writeToFile("CompressorTest_Colour.jpg", false);
    }

    private void writeToFile(String fileName, boolean useGreyScale) throws ImageCompressException, IOException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        String filePath = "src/test/resources/" + fileName;

        ImageCompressor instance = new ImageCompressor(config);
        FileUtils.writeByteArrayToFile(new File("src/test/resources/" + fileName),
            instance.compressBufferedImage(baseImageNormal, TARGET_IMAGE_SIZE, useGreyScale));

        File file = new File(filePath);
        assertTrue(file.exists());
    }

    @Test
    public void testWithColourHiResReject() {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        try {
            instance.compressBufferedImage(baseImageNormal, 100, false);
            fail("should have thrown an error");

        } catch (ImageCompressException e) {
            assertThat("expecting a custom exception", e.getMessage(), containsString("Image is too large for processing"));
        }
    }

    @Test
    public void successWithScanPdfDPI300() throws ImagePayloadException, IOException, ImageCompressException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        assertNotNull(instance.compressBufferedImage(
            ImageIO.read(
                new ByteArrayInputStream(PdfImageExtractor.extractImage(standardPdfDoc, 300))),
            500, false));

    }

    @Test
    public void failureWithScanPdfDPI600() throws ImagePayloadException, IOException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        try {

            instance.compressBufferedImage(ImageIO.read(new ByteArrayInputStream(PdfImageExtractor.extractImage(standardPdfDoc, 600))), 500, false);
            fail("should have thrown an error");

        } catch (ImageCompressException e) {
            assertThat("expecting a custom exception", e.getMessage(), containsString("Image is too large for processing"));
        }
    }
}