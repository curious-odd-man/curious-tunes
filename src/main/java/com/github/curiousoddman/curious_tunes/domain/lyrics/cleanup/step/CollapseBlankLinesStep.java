package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.step;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Collapses runs of 2 or more consecutive blank lines into a single blank line.
 * Also strips leading and trailing blank lines from the whole song.
 * <p>
 * Data shows max consecutive blanks in this dataset is 2, but this handles any number.
 */

// AI Generated
@Component
@Order(4)
public class CollapseBlankLinesStep implements CleaningStep {

    @Override
    public List<String> apply(List<String> lyrics) {
        List<String> result = new ArrayList<>();
        boolean prevBlank = false;

        for (String line : lyrics) {
            boolean isBlank = line.isBlank();
            if (isBlank && prevBlank) {
                continue; // skip consecutive blank
            }
            result.add(line);
            prevBlank = isBlank;
        }

        // strip leading blank lines
        while (!result.isEmpty() && result.getFirst().isBlank()) {
            result.removeFirst();
        }

        // strip trailing blank lines
        while (!result.isEmpty() && result.getLast().isBlank()) {
            result.removeLast();
        }

        return result;
    }
}
