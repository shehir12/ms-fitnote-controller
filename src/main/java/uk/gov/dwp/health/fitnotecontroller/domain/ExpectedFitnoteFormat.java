package uk.gov.dwp.health.fitnotecontroller.domain;

import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;
import uk.gov.dwp.health.fitnotecontroller.exception.FuzzyStringMatchException;
import uk.gov.dwp.health.fitnotecontroller.utils.FuzzyStringMatch;
import org.slf4j.Logger;
import uk.gov.dwp.logging.DwpEncodedLogger;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpectedFitnoteFormat {
    private static final Logger LOG = DwpEncodedLogger.getLogger(ExpectedFitnoteFormat.class.getName());

    public enum StringLocation {
        TOP_LEFT,
        TOP_RIGHT,
        BASE_LEFT,
        BASE_RIGHT
    }

    public enum Status {
        INITIALISED,
        FAILED,
        PARTIAL,
        SUCCESS
    }

    private Map<StringLocation, StringToMatch> matchingStrings = new EnumMap<>(StringLocation.class);
    private String baseLeftStringFound;
    private String baseRightStringFound;
    private String topLeftStringFound;
    private String topRightStringFound;
    private BufferedImage finalImage;
    private Status localStatus;
    private int diagonalTarget;
    private int highTarget;

    public ExpectedFitnoteFormat(FitnoteControllerConfiguration config) {
        initialise(config);
    }

    @java.lang.SuppressWarnings("squid:S1611")
    //Isolating the item(s) being acted on in () for a forEach reads easier than removing them. () are used for a Map (k,v) with
    //the -> acting on the contents of the brackets. Whilst they are required in that case it makes sense to standardise the forEach syntax.
    public void initialise(FitnoteControllerConfiguration config) {
        matchingStrings.clear();
        config.getTopLeftText().forEach((v) -> addString(StringLocation.TOP_LEFT, v));
        config.getTopRightText().forEach((v) -> addString(StringLocation.TOP_RIGHT, v));
        config.getBaseLeftText().forEach((v) -> addString(StringLocation.BASE_LEFT, v));
        config.getBaseRightText().forEach((v) -> addString(StringLocation.BASE_RIGHT, v));
        setStatus(Status.INITIALISED);
        setDiagonalTarget(config.getDiagonalTarget());
        setHighTarget(config.getHighTarget());
        setFinalImage(null);
    }

    public BufferedImage getFinalImage() {
        return finalImage;
    }

    public void setFinalImage(BufferedImage finalImage) {
        this.finalImage = finalImage;
    }

    private void addString(StringLocation inputLocation, String inputString) {
        if (matchingStrings.get(inputLocation) == null) {
            matchingStrings.put(inputLocation, new StringToMatch(inputString.toUpperCase()));
        } else {
            matchingStrings.get(inputLocation).setupString(inputString.toUpperCase());
        }
    }

    private int getHighTarget() {
        return highTarget;
    }

    private void setHighTarget(int input) {
        highTarget = input;
    }

    private int getDiagonalTarget() {
        return diagonalTarget;
    }

    private void setDiagonalTarget(int diagonalTarget) {
        this.diagonalTarget = diagonalTarget;
    }

    private void setStatus(Status input) {
        localStatus = input;
    }

    public Status getStatus() {
        return localStatus;
    }

    public void scanTopLeft(String topLeftString) {
        topLeftStringFound = topLeftString.toUpperCase();
        try {
            for (String tempLeft : matchingStrings.get(StringLocation.TOP_LEFT).getStringToFind()) {
                matchingStrings.get(StringLocation.TOP_LEFT).setupPercentage(FuzzyStringMatch.fuzzyStringContains(topLeftString, tempLeft));
            }
        } catch (FuzzyStringMatchException e) {
            LOG.debug(e.getClass().getName(), e);
        }
    }

    public void scanTopRight(String topRightString) {
        topRightStringFound = topRightString.toUpperCase();
        try {
            for (String tempRight : matchingStrings.get(StringLocation.TOP_RIGHT).getStringToFind()) {
                matchingStrings.get(StringLocation.TOP_RIGHT).setupPercentage(FuzzyStringMatch.fuzzyStringContains(topRightString, tempRight));
            }
        } catch (FuzzyStringMatchException e) {
            LOG.debug(e.getClass().getName(), e);
        }
    }

    public void scanBaseLeft(String baseLeftString) {
        baseLeftStringFound = baseLeftString.toUpperCase();
        try {
            for (String tempLeft : matchingStrings.get(StringLocation.BASE_LEFT).getStringToFind()) {
                matchingStrings.get(StringLocation.BASE_LEFT).setupPercentage(FuzzyStringMatch.fuzzyStringContains(baseLeftString, tempLeft));
            }
        } catch (FuzzyStringMatchException e) {
            LOG.debug(e.getClass().getName(), e);
        }
    }

    public void scanBaseRight(String baseRightString) {
        baseRightStringFound = baseRightString.toUpperCase();
        try {
            for (String tempRight : matchingStrings.get(StringLocation.BASE_RIGHT).getStringToFind()) {
                matchingStrings.get(StringLocation.BASE_RIGHT).setupPercentage(FuzzyStringMatch.fuzzyStringContains(baseRightString, tempRight));
            }
        } catch (FuzzyStringMatchException e) {
            LOG.debug(e.getClass().getName(), e);
        }
    }

    public int getTopLeftPercentage() {
        return getItemPercentage(StringLocation.TOP_LEFT);
    }

    public int getBaseLeftPercentage() {
        return getItemPercentage(StringLocation.BASE_LEFT);
    }

    public int getTopRightPercentage() {
        return getItemPercentage(StringLocation.TOP_RIGHT);
    }

    public int getBaseRightPercentage() {
        return getItemPercentage(StringLocation.BASE_RIGHT);
    }

    public String getLoggingString() {
        return String.format("%s TL:%d BR:%d BL:%d TR:%d",
                validateFitnotePassed().toString(),
                getTopLeftPercentage(),
                getBaseRightPercentage(),
                getBaseLeftPercentage(),
                getTopRightPercentage());
    }

    private int getItemPercentage(StringLocation itemLocation) {
        return matchingStrings.get(itemLocation).getPercentageFound();
    }

    public String getTopLeftStringToLog() {
        return topLeftStringFound == null ? "" : topLeftStringFound;
    }

    public String getBaseLeftStringToLog() {
        return baseLeftStringFound == null ? "" : baseLeftStringFound;
    }

    public String getTopRightStringToLog() {
        return topRightStringFound == null ? "" : topRightStringFound;
    }

    public String getBaseRightStringToLog() {
        return baseRightStringFound == null ? "" : baseRightStringFound;
    }

    public Status validateFitnotePassed() {
        if (checkHighMarks() > 0) {
            if (validateDiagonals() || leftHandSide()) {
                setStatus(Status.SUCCESS);

            } else {
                setStatus(Status.PARTIAL);
            }
        } else {
            setStatus(Status.FAILED);
        }
        return getStatus();
    }

    private boolean leftHandSide() {
        return (getTopLeftPercentage() >= getHighTarget()) && (getTopRightPercentage() >= getHighTarget());
    }

    private int checkHighMarks() {
        AtomicInteger zeroCount = new AtomicInteger(0);
        matchingStrings.forEach((k, v) -> {
            if (v.getPercentageFound() >= getHighTarget()) {
                zeroCount.incrementAndGet();
            }
        });
        return zeroCount.get();
    }

    private boolean validateDiagonals() {
        return validateDiagonal(getTopLeftPercentage(), getBaseRightPercentage()) || validateDiagonal(getTopRightPercentage(), getBaseLeftPercentage());
    }

    private boolean validateDiagonal(int input1, int input2) {
        return ((input1 >= getHighTarget()) && (input2 >= getDiagonalTarget())) || ((input2 >= getHighTarget()) && (input1 >= getDiagonalTarget()));
    }
}
