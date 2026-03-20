package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.step;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Strips the trailing footer block injected by lyrics sites.
 * Walks from the bottom upward, removing lines while they match the footer set.
 * Stops at the first real content line.
 * <p>
 * Handles partial footers — some songs are missing one or two of the three lines.
 * <p>
 * Known footer lines:
 * Explain
 * Request
 * ×
 * (fade)
 * (blank lines)
 */
// AI Generated

@Component
@Order(2)
public class FooterStripStep implements CleaningStep {
    private static final Set<String> FOOTER_LINES = Set.of(
            "explain", "request", "×", "(fade)", "(fade out)", "(fades out)", "(fades)"
    );

    @Override
    public List<String> apply(List<String> lyrics) {
        List<String> lines = new ArrayList<>(lyrics);

        while (!lines.isEmpty()) {
            String last = lines.getLast().strip();
            if (last.isEmpty() || FOOTER_LINES.contains(last.toLowerCase())) {
                lines.removeLast();
            } else {
                break;
            }
        }

        return lines;
    }
}
