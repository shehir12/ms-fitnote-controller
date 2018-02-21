package fitnotecontroller.utils;

import fitnotecontroller.exception.FuzzyStringMatchException;
import gov.dwp.utilities.logging.DwpEncodedLogger;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class FuzzyStringMatch {
    private static final Logger LOGGER = DwpEncodedLogger.getLogger(FuzzyStringMatch.class.getName());
    private static Map<Character, String> charList;
    private static final int SPACE_CHARACTER = 32;
    private static final int PERFECT = 100;

    static {
        charList = new HashMap<>();
        charList.put('ß', "ss");
        charList.put('\uFB00', "ff");
        charList.put('\uFB01', "fi");
        charList.put('\uFB02', "fl");
        charList.put('\uFB03', "ffi");
        charList.put('\uFB04', "ffl");
        charList.put('\uFB05', "ft");
        charList.put('\uFB06', "st");
        charList.put('\u00E0', "a");
        charList.put('\u00E1', "a");
        charList.put('\u00E2', "a");
        charList.put('\u00E3', "a");
        charList.put('\u00E4', "a");
        charList.put('\u00E5', "a");
        charList.put('\u00E6', "ae");
        charList.put('\u00E8', "e");
        charList.put('\u00E9', "e");
        charList.put('\u00EA', "e");
        charList.put('\u00EB', "e");
        charList.put('\u00EC', "i");
        charList.put('\u00ED', "i");
        charList.put('\u00EE', "i");
        charList.put('\u00EF', "i");
        charList.put('\u00F0', "o");
        charList.put('\u00F1', "n");
        charList.put('\u00F2', "o");
        charList.put('\u00F3', "o");
        charList.put('\u00F4', "o");
        charList.put('\u00F5', "o");
        charList.put('\u00F6', "o");
        charList.put('\u00F9', "u");
        charList.put('\u00FA', "u");
        charList.put('\u00FB', "u");
        charList.put('\u00FC', "u");
    }

    private FuzzyStringMatch() {
    }

    public static boolean fuzzyStringValid(String inspected, String lookingFor, int percentageRequired) throws FuzzyStringMatchException {
        return (percentageRequired) <= fuzzyStringContains(inspected, lookingFor);
    }

    public static int fuzzyStringContains(String inspected, String lookingFor) throws FuzzyStringMatchException {
        if (!isValidString(inspected)) {
            throw new FuzzyStringMatchException(String.format("invalid input strings : inspected : '%s'", inspected));
        }
        if (!isValidString(lookingFor)) {
            throw new FuzzyStringMatchException(String.format("invalid input strings : lookingFor : '%s'", lookingFor));
        }

        String localLookingFor = !lookingFor.matches("^[A-Za-z .,;:'0-9]+") ? replaceInvalidCharacters(lookingFor).toUpperCase() : lookingFor.toUpperCase();
        String localInspected = !inspected.matches("^[A-Za-z .,;:'0-9]+") ? replaceInvalidCharacters(inspected).toUpperCase() : inspected.toUpperCase();
        int returnValue = 0;
        int charIndex = 0;

        while ((charIndex + localLookingFor.length()) <= localInspected.length() && (returnValue < PERFECT)) {
            String workingString = localInspected.substring(charIndex, charIndex + localLookingFor.length());

            int matchValue = workingString.equals(localLookingFor) ? PERFECT : fuzzyStringsMatch(workingString, localLookingFor);
            if (matchValue > returnValue) {
                returnValue = matchValue;
            }

            // just to show the string traversing the inspected list
            LOGGER.debug(String.format(">%s< :: >%s< = %d", localLookingFor, workingString, matchValue));

            charIndex++;
        }

        return returnValue;
    }

    protected static int fuzzyStringsMatch(String inspected, String lookingFor) throws FuzzyStringMatchException {
        int returnValue = 0;

        String toInspect = inspected;

        if (lookingFor.length() != toInspect.length()) {
            throw new FuzzyStringMatchException("string length does not match");
        }

        int length = lookingFor.length();

        int loop = 0;
        char[] inspectedArray = toInspect.toCharArray();
        for (char letter : inspectedArray) {
            if (letter == lookingFor.charAt(loop)) {
                if (letter == SPACE_CHARACTER) {
                    length--;
                } else {
                    returnValue++;
                }
            }
            loop++;
        }
        return (returnValue * PERFECT) / length;
    }

    protected static boolean isValidString(String inputString) {
        return (null != inputString) && (!inputString.isEmpty()) && (inputString.length() > 0);
    }

    protected static String replaceInvalidCharacters(String inspected) throws FuzzyStringMatchException {
        if ((null == charList) || (charList.isEmpty())) {
            throw new FuzzyStringMatchException("charList initialisation failed");
        }
        String localInspected = inspected;
        char[] inspectedArray = localInspected.toCharArray();
        int count = 0;
        String tempLetter;
        for (char letter : inspectedArray) {
            tempLetter = String.format("%c", letter);
            if (!tempLetter.matches("^[A-Za-z .,;:'0-9]")) {
                String replacingWith = charList.get(letter);
                if (replacingWith == null) {
                    replacingWith = " ";
                }
                localInspected = localInspected.substring(0, count) + replacingWith + localInspected.substring(count + 1, localInspected.length() - replacingWith.length() + 1);
            }
            count++;
        }
        return localInspected;
    }
}
