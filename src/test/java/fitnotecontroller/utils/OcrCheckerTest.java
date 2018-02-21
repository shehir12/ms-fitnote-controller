package fitnotecontroller.utils;

import fitnotecontroller.application.FitnoteControllerConfiguration;
import fitnotecontroller.domain.ExpectedFitnoteFormat;
import fitnotecontroller.domain.ImagePayload;
import gherkin.deps.net.iharder.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OcrCheckerTest {
    private OcrChecker checker;

    @Mock
    private FitnoteControllerConfiguration mockConfig;

    @Before
    public void setup() {
        List<String> topLeftList = new LinkedList<>();
        List<String> topRightList = new LinkedList<>();
        List<String> baseRightList = new LinkedList<>();
        List<String> baseLeftList = new LinkedList<>();
        topLeftList.add("Understand user needs");
        topRightList.add("end-to-end service");
        topRightList.add("all common browsers");
        baseLeftList.add("all new source code open");
        baseRightList.add("Performance Platform");
        baseRightList.add("beginning to end");

        when(mockConfig.getTesseractFolderPath()).thenReturn("src/main/properties");
        when(mockConfig.getBorderLossPercentage()).thenReturn(10);
        when(mockConfig.getMaxLogChars()).thenReturn(2000);
        when(mockConfig.getTargetBrightness()).thenReturn(179);
        when(mockConfig.getDiagonalTarget()).thenReturn(20);
        when(mockConfig.getHighTarget()).thenReturn(100);
        when(mockConfig.getContrastCutOff()).thenReturn(105);
        when(mockConfig.getTopLeftText()).thenReturn(topLeftList);
        when(mockConfig.getTopRightText()).thenReturn(topRightList);
        when(mockConfig.getBaseLeftText()).thenReturn(baseLeftList);
        when(mockConfig.getBaseRightText()).thenReturn(baseRightList);

        checker = new OcrChecker(mockConfig);
    }

    private ImagePayload getTestImage(String imageFile) throws IOException {
        String imageString = Base64.encodeFromFile(this.getClass().getResource(imageFile).getPath());
        ImagePayload payload = new ImagePayload();
        payload.setImage(imageString);
        payload.setSessionId(UUID.randomUUID().toString());
        return payload;
    }

    @Test
    public void confirmOcrCheckerCanRecogniseText() throws IOException {
        assertThat(checker.imageContainsReadableText(getTestImage("/OcrTest.jpg")), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
    }

    @Test
    public void confirmOcrCheckerCanRecogniseUpsideDownText() throws IOException {
        assertThat(checker.imageContainsReadableText(getTestImage("/OcrTest_UpsideDown.jpg")), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
    }

    @Test
    public void confirmOcrCheckerDoesNotRecogniseNonExistentText() throws IOException {
        assertThat(checker.imageContainsReadableText(getTestImage("/EmptyPage.jpg")), is(equalTo(ExpectedFitnoteFormat.Status.FAILED)));
    }

    @Test
    public void confirmOcrCheckerPartialOnHalfPage() throws IOException {
        assertThat(checker.imageContainsReadableText(getTestImage("/OcrTest_LHS.jpg")), is(equalTo(ExpectedFitnoteFormat.Status.PARTIAL)));
    }

    @Test
    public void rightHandSidePageIs_NOT_ACCEPTED() throws IOException {
        assertThat(checker.imageContainsReadableText(getTestImage("/OcrTest_RHS.jpg")), is(equalTo(ExpectedFitnoteFormat.Status.FAILED)));
    }

    @Test
    public void confirmImagePayloadDoesNotContainOriginalImageAfterSuccessfulTextRecognition() throws IOException {
        ImagePayload payload = getTestImage("/OcrTest.jpg");
        String incorrectImage = payload.getImage();
        checker.imageContainsReadableText(payload);
        assertNotEquals(incorrectImage, payload.getImage());
    }

    @Test
    public void confirmNewFitnoteFormatWorked() throws IOException {
        ImagePayload payload = getTestImage("/OcrTest_New.jpg");
        assertThat(checker.imageContainsReadableText(payload), is(equalTo(ExpectedFitnoteFormat.Status.SUCCESS)));
    }

    @Test
    public void confirmPixi3ImageFails() throws IOException {
        ImagePayload payload = getTestImage("/Pixi3.jpg");
        assertThat(checker.imageContainsReadableText(payload), is(equalTo(ExpectedFitnoteFormat.Status.FAILED)));
    }

    @Test
    public void testTesseractFailure() throws IOException {
        when(mockConfig.getTesseractFolderPath()).thenReturn("");
        ImagePayload payload = getTestImage("/FullPage_Portrait.jpg");
        try {
            checker.imageContainsReadableText(payload);
            assertFalse(true);//should never get here!
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("the tessdata configuration file could not be found in"));
        }
    }
}