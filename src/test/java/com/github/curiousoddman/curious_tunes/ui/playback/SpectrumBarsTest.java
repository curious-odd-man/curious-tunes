package com.github.curiousoddman.curious_tunes.ui.playback;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectrumBarsTest {

    @Test
    void bandRangesCoverTheShownBandsWithoutGapsOrEmptyBars() {
        int[][] ranges = SpectrumBars.bandRanges(48, 128);
        int[]   first  = ranges[0];
        int[]   end    = ranges[1];
        assertEquals(0, first[0]);
        assertEquals(96, end[47]);                  // 75% of 128 bands
        for (int i = 0; i < 48; i++) {
            assertTrue(end[i] > first[i], "bar " + i + " has no band");
            if (i > 0) {
                assertEquals(end[i - 1], first[i], "gap or overlap before bar " + i);
            }
        }
        // Log spacing: the treble bars group more bands than the bass bars.
        assertEquals(1, end[0] - first[0]);
        assertTrue(end[47] - first[47] > end[0] - first[0]);
    }

    @Test
    void barsShareBandsWhenThereAreFewerBandsThanBars() {
        int[][] ranges = SpectrumBars.bandRanges(48, 16);
        for (int i = 0; i < 48; i++) {
            assertTrue(ranges[0][i] >= 0 && ranges[1][i] <= 16);
            assertEquals(1, ranges[1][i] - ranges[0][i]);
        }
    }

    @Test
    void barRisesQuicklyAndFallsSlowly() {
        SpectrumBars bars = new SpectrumBars(4);
        bars.setSpectrum(loud(128), -80);
        bars.step(0.1);
        assertTrue(bars.getLevels()[0] > 0.9, "should be near full after 0.1 s: " + bars.getLevels()[0]);

        bars.silence();
        bars.step(0.1);
        assertTrue(bars.getLevels()[0] > 0.7, "should still be high after 0.1 s of silence: " + bars.getLevels()[0]);
    }

    @Test
    void peakHoldsThenFallsAndEverythingReachesIdle() {
        SpectrumBars bars = new SpectrumBars(4);
        bars.setSpectrum(loud(128), -80);
        for (int i = 0; i < 5; i++) {
            bars.step(0.04);
        }
        double peak = bars.getPeaks()[0];
        bars.silence();
        bars.step(0.04);
        assertEquals(peak, bars.getPeaks()[0], 1e-9);          // held
        assertTrue(bars.getPeaks()[0] >= bars.getLevels()[0]);
        assertFalse(bars.isIdle());

        for (int i = 0; i < 100; i++) {
            bars.step(0.04);
        }
        assertTrue(bars.isIdle());
    }

    @Test
    void silentSpectrumGivesZeroTargets() {
        SpectrumBars bars    = new SpectrumBars(48);
        float[]      silence = new float[128];
        Arrays.fill(silence, -80f);
        bars.setSpectrum(silence, -80);
        bars.step(0.04);
        for (double level : bars.getLevels()) {
            assertEquals(0, level, 1e-9);
        }
        assertTrue(bars.isIdle());
    }

    private static float[] loud(int bands) {
        float[] magnitudes = new float[bands];
        Arrays.fill(magnitudes, -2f);
        return magnitudes;
    }
}
