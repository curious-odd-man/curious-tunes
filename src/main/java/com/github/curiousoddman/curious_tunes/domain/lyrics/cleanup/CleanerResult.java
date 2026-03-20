package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup;

import com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.flagger.Flag;

import java.util.List;
import java.util.Objects;

// AI Generated
public record CleanerResult(
        String original,
        String cleaned,
        List<Flag> flags
) {
    public boolean hasFlags() {
        return !flags.isEmpty();
    }

    public boolean wasModified() {
        return !Objects.equals(original, cleaned);
    }
}