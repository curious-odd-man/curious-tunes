package com.github.curiousoddman.curious_tunes.ui.playback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LyricsFitterTest {

    @Test
    void usesMaxSizeWhenEverythingFits() {
        assertEquals(48, LyricsFitter.fit(8, 48, 1000, size -> size * 20));
    }

    @Test
    void usesMinSizeWhenNothingFits() {
        assertEquals(8, LyricsFitter.fit(8, 48, 100, size -> size * 20));
    }

    @Test
    void findsLargestSizeThatFits() {
        double size = LyricsFitter.fit(8, 48, 600, s -> s * 20);      // exact answer: 30
        assertTrue(size * 20 <= 600);
        assertTrue(size >= 29.5);
    }

    @Test
    void worksWithNonLinearHeights() {
        double size = LyricsFitter.fit(8, 48, 500, s -> s * s * 0.5); // exact answer: ~31.6
        assertTrue(size * size * 0.5 <= 500);
        assertTrue(size >= 31);
    }
}
