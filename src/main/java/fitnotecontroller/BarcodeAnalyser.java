package fitnotecontroller;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.decoder.Decoder;
import com.google.zxing.datamatrix.detector.Detector;
import fitnotecontroller.domain.BarcodeContents;
import fitnotecontroller.exception.QRExtractionException;
import fitnotecontroller.utils.ImageUtils;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BarcodeAnalyser {
    private static final Logger LOG = DwpEncodedLogger.getLogger(BarcodeAnalyser.class.getName());
    private BufferedImage fitnoteImage;

    public BarcodeAnalyser(String fullPageImage) throws IOException {
        this.fitnoteImage = ImageIO.read(new ByteArrayInputStream(Base64.decodeBase64(fullPageImage)));
    }

    public BarcodeContents decodeBarcodeContents() {
        BarcodeContents dataContents = null;
        try {
            BufferedImageLuminanceSource imageSource = new BufferedImageLuminanceSource(ImageUtils.createRotatedCopy(getFitnoteImage(), 0));
            BitMatrix bm = new BinaryBitmap(new HybridBinarizer(imageSource)).getBlackMatrix();
            DetectorResult detectorResult = new Detector(bm).detect();
            Decoder decoder = new Decoder();
            DecoderResult decoderResult = decoder.decode(detectorResult.getBits());
            dataContents = new BarcodeContents(decoderResult.getText());

        } catch (ChecksumException | FormatException | NotFoundException e) {
            LOG.error(String.format("zxing exception :: %s", e.getMessage()));
            LOG.debug(e);
        } catch (QRExtractionException e) {
            LOG.error(String.format("barcode decoding exception :: %s", e.getMessage()));
            LOG.debug(e);
        }

        LOG.info(String.format("%sBarcode data extracted", dataContents != null ? "" : "No "));
        return dataContents;
    }

    public BufferedImage getFitnoteImage() {
        return fitnoteImage;
    }


}
