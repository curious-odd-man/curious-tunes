package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.step;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits lines that were incorrectly joined together.
 * Detects a lowercase letter immediately followed by an uppercase letter
 * with no space, and inserts a newline at that boundary.
 * <p>
 * Handles both Latin and Cyrillic scripts.
 * <p>
 * Examples:
 * "And I looked into your eyesCaptivated by your smile"
 * → "And I looked into your eyes"
 * "Captivated by your smile"
 * <p>
 * "The word goes from land to landThe dawn of enlightenment"
 * → "The word goes from land to land"
 * "The dawn of enlightenment"
 * <p>
 * Note: this runs last so blank-line collapsing has already processed
 * the original structure before new lines are introduced.
 */
@Component
@Order(5)
// AI Generated
public class GluedLineStep implements CleaningStep {
    // matches lowercase (Latin or Cyrillic) immediately followed by uppercase (Latin or Cyrillic)
    private static final Pattern GLUE_PATTERN =
            Pattern.compile("([a-zа-яё])([A-ZА-ЯЁ])");

    @Override
    public List<String> apply(List<String> lyrics) {
        return lyrics.stream()
                .flatMap(line -> Arrays.stream(GLUE_PATTERN.matcher(line).replaceAll("$1\n$2").split("\n")))
                .toList();
    }
}
