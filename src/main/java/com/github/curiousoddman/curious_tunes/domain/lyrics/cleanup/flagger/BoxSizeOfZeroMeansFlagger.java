package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.flagger;

import org.springframework.stereotype.Component;

/**
 * Flags songs that contain known database/export artifact text.
 * These songs are likely broken or empty and should be skipped entirely.
 * <p>
 * Known artifact: "box size of zero means 'till end of file. That is not yet supported"
 */
// AI Generated

@Component
public class BoxSizeOfZeroMeansFlagger implements SuspicionFlagger {

    private static final String ARTIFACT_MARKER =
            "box size of zero means";

    @Override
    public boolean test(String lyrics) {
        return lyrics.contains(ARTIFACT_MARKER);
    }

    @Override
    public Flag flag() {
        return Flag.PARSING_ERROR;
    }
}
