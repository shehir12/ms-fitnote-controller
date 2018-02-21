package fitnotecontroller.utils;

import fitnotecontroller.exception.FuzzyStringMatchException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FuzzyStringMatchTest {
    private static final String NINTY_PERCENT_MATCH_BIG_STRING = "KlMnOpQrAbcDe1234X67890fGhIj";
    private static final String NINTY_PERCENT_MATCH_SAME_LENGTH = "XbcDe12345";
    private static final String SEVENTY_PERCENT_MATCH_SAME_LENGTH = "XXXDe12345";

    private static final String STRING_TO_FIND = "AbcDe12345";
    private static final int PERCENTAGE_HAPPY_WITH = 90;

    @Test
    public void validate_FuzzyStringContains_Returns_100_With_SingleMatch_Char() throws FuzzyStringMatchException {
        assertThat("Expecting 100% match", FuzzyStringMatch.fuzzyStringContains("a", "a"), is(equalTo(100)));
    }

    @Test(expected = FuzzyStringMatchException.class)
    public void validate_FuzzyStringContains_Throws_Exception_When_Given_Null() throws FuzzyStringMatchException {
        FuzzyStringMatch.fuzzyStringContains(null, "lookingFor");
    }

    @Test(expected = FuzzyStringMatchException.class)
    public void validate_FuzzyStringContains_Throws_Exception_When_Given_Second_Null() throws FuzzyStringMatchException {
        FuzzyStringMatch.fuzzyStringContains("inspected", null);
    }

    @Test
    public void validate_FuzzyStringValid_Returns_True_Given_70Pct_String() throws Exception {
        assertTrue(FuzzyStringMatch.fuzzyStringValid(SEVENTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND, 70));
    }

    @Test
    public void validate_FuzzyStringValid_Returns_False_Given_71_Char_70Pct_String() throws Exception {
        assertFalse("70% string should fail 71% threshold", FuzzyStringMatch.fuzzyStringValid(SEVENTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND, 71));
    }

    @Test
    public void validate_FuzzyStringValid_Returns_TRUE_Given_90Pct_Same_Length_String() throws Exception {
        assertTrue(FuzzyStringMatch.fuzzyStringValid(NINTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND, PERCENTAGE_HAPPY_WITH));
    }

    @Test
    public void validate_FuzzyStringValid_Returns_TRUE_Given_90Pct_Big_String() throws Exception {
        assertTrue(FuzzyStringMatch.fuzzyStringValid(NINTY_PERCENT_MATCH_BIG_STRING, STRING_TO_FIND, PERCENTAGE_HAPPY_WITH));
    }

    @Test
    public void validate_FuzzyStringContains_Given_KlMnOpQrAbcDe1234S67890fGhIj() throws Exception {
        assertEquals(90, FuzzyStringMatch.fuzzyStringContains(NINTY_PERCENT_MATCH_BIG_STRING, STRING_TO_FIND));
    }//this contains a string that matches 90%

    @Test
    public void validate_FuzzyStringContains_GivenStuff() throws Exception {
        assertEquals(75, FuzzyStringMatch.fuzzyStringContains("aaccccaabbccaaaabbccaabcdaaaabbbbccbbbbbc", "ebcd"));
    }//this contains a string that matches 90%

    @Test
    public void validate_FuzzyStringsMatch_Given_AbcDe12340() throws FuzzyStringMatchException {
        assertEquals(90, FuzzyStringMatch.fuzzyStringsMatch(NINTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND));
    }

    @Test
    public void validate_FuzzyStringsMatch_Given_abcde12340() throws FuzzyStringMatchException {
        assertTrue(FuzzyStringMatch.fuzzyStringsMatch(SEVENTY_PERCENT_MATCH_SAME_LENGTH, STRING_TO_FIND) == 70);
    }

    @Test
    public void verify_IsValidString_Returns_False_Given_Null() {
        assertFalse(FuzzyStringMatch.isValidString(null));
    }

    @Test
    public void verify_IsValidString_Returns_False_Given_EmptyString() {
        assertFalse(FuzzyStringMatch.isValidString(new String()));
    }

    @Test
    public void verify_IsValidString_Returns_False_Given_Double_Quotes() {
        assertFalse(FuzzyStringMatch.isValidString(""));
    }

    @Test
    public void verify_IsValidString_Returns_True_Given_ABC() {
        assertTrue(FuzzyStringMatch.isValidString("ABC"));
    }

    @Test
    public void verify_replaceInvalidCharacters_Returns_FLOSS_When_Given_Floß_Space() throws Exception {
        assertEquals("Floss", FuzzyStringMatch.replaceInvalidCharacters("Floß "));
    }

    @Test
    public void verify_replaceInvalidCharacters_Returns_Fitnote_When_Given_Fitnoté() throws Exception {
        assertEquals("Fitnote", FuzzyStringMatch.replaceInvalidCharacters("Fitnoté"));
    }

    @Test
    public void verify_replaceInvalidCharacters() throws Exception {
        assertEquals("Fitn te", FuzzyStringMatch.replaceInvalidCharacters("Fitn%té"));
    }

    @Test
    public void testPhraseMatchingInScanFromExample() throws FuzzyStringMatchException {
        int scanMatchNew = FuzzyStringMatch.fuzzyStringContains("  \ndsgfhdhkjesdfghj    \tmary hid _ little limb _?!##wsdfghytrdefg".toUpperCase(), "MARY HAD A LITTLE LAMB");
        assertThat("should be 92 percent", scanMatchNew, is(equalTo(83)));
    }
}