package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.step;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Strips band/artist name credit lines injected by lyrics sites.
 * These appear as the last non-empty line in the format "BAND NAME LYRICS".
 * <p>
 * Examples:
 * "DREAM THEATER LYRICS"
 * "DIO LYRICS"
 * "RAINBOW LYRICS"
 * <p>
 * Pattern: one or more ALL-CAPS words followed by the word LYRICS.
 */
// AI Generated

@Component
@Order(3)
public class BandNameFooterStep implements CleaningStep {
    private static final Pattern BAND_NAME_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9 .'-]+LYRICS$");

    @Override
    public List<String> apply(List<String> lyrics) {
        List<String> lines = new ArrayList<>(lyrics);

        // skip trailing blanks to find the last content line
        int lastContentIndex = lines.size() - 1;
        while (lastContentIndex >= 0 && lines.get(lastContentIndex).isBlank()) {
            lastContentIndex--;
        }

        if (lastContentIndex >= 0) {
            String lastContent = lines.get(lastContentIndex).strip();
            if (BAND_NAME_PATTERN.matcher(lastContent).matches()) {
                lines.remove(lastContentIndex);
            }
        }

        return lines;
    }
}
