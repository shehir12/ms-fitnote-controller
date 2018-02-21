package fitnotecontroller.utils;

import fitnotecontroller.exception.ImagePayloadException;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class PdfImageExtractor {
    private static final Logger LOG = DwpEncodedLogger.getLogger(PdfImageExtractor.class.getName());

    private PdfImageExtractor() {
    }

    public static byte[] extractImage(byte[] incomingObject, int scanDPI) throws ImagePayloadException {
        if (incomingObject == null) {
            throw new ImagePayloadException("incoming byte array cannot be null");
        }

        byte[] extractedImage = null;
        PDDocument document = null;
        try {

            LOG.debug("loading and rendering pdf in order to copy page to image");
            document = PDDocument.load(incomingObject);
            PDFRenderer renderer = new PDFRenderer(document);

            BufferedImage imageBuffer = renderer.renderImageWithDPI(0, scanDPI, ImageType.RGB);
            LOG.info(String.format("scanned page 1 to BufferedImage at %d DPI", scanDPI));

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(imageBuffer, "jpg", outputStream);
                extractedImage = outputStream.toByteArray();
            }

        } catch (IOException e) {
            LOG.error(String.format("Incoming byte array is not a pdf file :: %s", e.getMessage()));
            LOG.debug(e);

        } catch (IllegalArgumentException e) {
            LOG.error(String.format("Error with pdf scanning operation :: %s", e.getMessage()));
            LOG.debug(e);

        } finally {
            if (document != null) {
                try {
                    document.close();

                } catch (IOException e) {
                    LOG.error(e.getMessage());
                    LOG.debug(e);
                }
            }
        }

        return extractedImage;
    }
}
