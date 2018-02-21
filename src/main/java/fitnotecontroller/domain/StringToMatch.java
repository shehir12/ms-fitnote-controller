package fitnotecontroller.domain;

import java.util.ArrayList;
import java.util.List;

public class StringToMatch {
    private List<String> stringsToFind = new ArrayList<>();
    private int percentageFound;

    public StringToMatch(String inputString) {
        resetPercentage();
        setupString(inputString);
    }

    public void setupString(String input) {
        stringsToFind.add(input);
    }

    public void setupPercentage(int input) {
        if (input > percentageFound) {
            percentageFound = input;
        }
    }

    public void resetPercentage() {
        percentageFound = -1;
    }

    public int getPercentageFound() {
        return percentageFound;
    }

    protected List<String> getStringToFind() {
        return stringsToFind;
    }
}
