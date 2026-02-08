package com.github.curiousoddman.curious_tunes.backend.lyrics;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;

@UtilityClass
public final class LyricsHelper {

    /**
     * Convert accented characters into non-accented characters.
     * Equivalent to Python's unicodedata.normalize('NFKD', ...).
     */
    public static String removeAccents(String input) {
        if (input == null) {
            return null;
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKD);

        // Remove combining diacritical marks
        return normalized.replaceAll("\\p{M}", "");
    }

    /**
     * Tests downloaded lyrics to detect licensing restriction messages
     * and ensures there are enough newline characters.
     */
    public static boolean testLyrics(String lyrics) {
        if (lyrics == null || lyrics.isEmpty()) {
            return false;
        }

        String licenseStr1 =
                "We are not in a position to display these lyrics due to licensing restrictions. Sorry for the inconvinience.";
        String licenseStr2 =
                "display these lyrics due to licensing restrictions";
        String licenseStr3 =
                "We are not in a position to display these lyrics due to licensing restrictions.\nSorry for the inconvinience.";

        // Check for licensing text or too few newline characters
        return !lyrics.contains(licenseStr1)
                && !lyrics.contains(licenseStr2)
                && !lyrics.contains(licenseStr3)
                && countOccurrences(lyrics, '\n') >= 4;
    }

    private static int countOccurrences(String text, char ch) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
}
