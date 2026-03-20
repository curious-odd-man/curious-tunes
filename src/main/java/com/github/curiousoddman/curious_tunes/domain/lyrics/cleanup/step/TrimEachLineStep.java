package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.step;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strips trailing whitespace from every line.
 * Must run first — other steps rely on clean line endings for their regex matching.
 * Example: "  (fade)                   " → "(fade)"
 */
@Component
@Order(1) // AI Generated

public class TrimEachLineStep implements CleaningStep {

    @Override
    public List<String> apply(List<String> lyrics) {
        return lyrics.stream()
                .map(String::trim)
                .toList();
    }
}
