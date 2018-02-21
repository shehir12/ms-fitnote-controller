package fitnotecontroller.utils;

import fitnotecontroller.application.FitnoteControllerConfiguration;
import fitnotecontroller.exception.ImageCompressException;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;

public class ImageCompressor {
    private static final Logger LOGGER = DwpEncodedLogger.getLogger(ImageCompressor.class.getName());
    private boolean rejectingOversizeImages;

    public ImageCompressor(FitnoteControllerConfiguration config) {
        this.rejectingOversizeImages = config.isRejectingOversizeImages();
    }

    public byte[] compressBufferedImage(BufferedImage inputImage, int targetImageSizeKB, boolean useGrayScale) throws ImageCompressException {
        LOGGER.info(String.format("Starting image compression :: RejectingOversizedImage = %s, TargetSizeKB = %d, GreyScale = %s", isRejectingOversizeImages(), targetImageSizeKB, useGrayScale));
        BufferedImage workingImage = useGrayScale ? turnGreyscale(inputImage) : inputImage;
        BigDecimal compressionQuality = BigDecimal.valueOf(1);
        try {

            byte[] jpegData = compressImage(workingImage, compressionQuality.floatValue());
            LOGGER.info(String.format("initial file size (bytes) = %d", jpegData.length));

            while ((jpegData.length > (targetImageSizeKB * 1000)) && (compressionQuality.doubleValue() > 0)) {

                if (compressionQuality.doubleValue() > 0.1) {
                    compressionQuality = compressionQuality.subtract(BigDecimal.valueOf(0.1));
                } else {
                    compressionQuality = compressionQuality.subtract(BigDecimal.valueOf(0.01));
                }

                jpegData = compressImage(workingImage, compressionQuality.floatValue());
                LOGGER.debug(String.format("jpg %d, compression %f", jpegData.length, compressionQuality.floatValue()));
            }

            if ((jpegData.length > (targetImageSizeKB * 1000)) && (isRejectingOversizeImages())) {
                throw new ImageCompressException("Image is too large for processing, try with less quality or turn 'rejectingOversizeImages' off");
            }

            LOGGER.info(String.format("Successfully completed image compression, result = %d bytes", jpegData.length));
            return jpegData;

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            LOGGER.debug(e);
            throw new ImageCompressException(e.getMessage());
        }
    }

    private byte[] compressImage(BufferedImage image, float compressionQuality) throws IOException {
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(compressionQuality);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed);
        jpgWriter.setOutput(outputStream);

        jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
        jpgWriter.dispose();

        return compressed.toByteArray();
    }

    private BufferedImage turnGreyscale(BufferedImage inputImage) {
        BufferedImage image = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = image.getGraphics();

        g.drawImage(inputImage, 0, 0, null);
        g.dispose();

        return image;
    }

    public boolean isRejectingOversizeImages() {
        return rejectingOversizeImages;
    }
}


