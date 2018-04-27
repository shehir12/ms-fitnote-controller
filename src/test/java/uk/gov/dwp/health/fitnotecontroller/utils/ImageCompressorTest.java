package uk.gov.dwp.health.fitnotecontroller.utils;

import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.exception.ImageCompressException;
import uk.gov.dwp.health.fitnotecontroller.exception.ImagePayloadException;
import uk.gov.dwp.health.fitnotecontroller.utils.ImageCompressor;
import uk.gov.dwp.health.fitnotecontroller.utils.PdfImageExtractor;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImageCompressorTest {
    private static BufferedImage baseImage_Normal;
    private static byte[] standardPdf_Doc;

    private final static int targetImageSize = 500;

    @Mock
    private FitnoteControllerConfiguration config;

    @BeforeClass
    public static void init() throws IOException {
        standardPdf_Doc = FileUtils.readFileToByteArray(new File("src/test/resources/FullPage_Portrait.pdf"));
        baseImage_Normal = ImageIO.read(new File("src/test/resources/FullPage_Portrait.jpg"));
    }

    @Before
    public void setup() {
        when(config.getTargetImageSizeKB()).thenReturn(targetImageSize);
    }

    @Test
    public void testWithGreyScale_Normal() throws ImageCompressException, IOException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        FileUtils.writeByteArrayToFile(new File("src/test/resources/CompressorTest_GS.jpg"), instance.compressBufferedImage(baseImage_Normal, targetImageSize, true));
    }

    @Test
    public void testWithColour_Normal() throws ImageCompressException, IOException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        FileUtils.writeByteArrayToFile(new File("src/test/resources/CompressorTest_Colour.jpg"), instance.compressBufferedImage(baseImage_Normal, targetImageSize, false));
    }

    @Test
    public void testWithColour_HiResReject() {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        try {
            instance.compressBufferedImage(baseImage_Normal, 100, false);
            fail("should have thrown an error");

        } catch (ImageCompressException e) {
            assertThat("expecting a custom exception", e.getMessage(), containsString("Image is too large for processing"));
            e.printStackTrace();
        }
    }

    @Test
    public void successWithScanPdf_DPI300() throws ImagePayloadException, IOException, ImageCompressException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        instance.compressBufferedImage(ImageIO.read(new ByteArrayInputStream(PdfImageExtractor.extractImage(standardPdf_Doc, 300))), 500, false);
    }

    @Test
    public void failureWithScanPdf_DPI600() throws ImagePayloadException, IOException {
        when(config.isRejectingOversizeImages()).thenReturn(true);

        ImageCompressor instance = new ImageCompressor(config);
        try {

            instance.compressBufferedImage(ImageIO.read(new ByteArrayInputStream(PdfImageExtractor.extractImage(standardPdf_Doc, 600))), 500, false);
            fail("should have thrown an error");

        } catch (ImageCompressException e) {
            assertThat("expecting a custom exception", e.getMessage(), containsString("Image is too large for processing"));
            e.printStackTrace();
        }
    }
}