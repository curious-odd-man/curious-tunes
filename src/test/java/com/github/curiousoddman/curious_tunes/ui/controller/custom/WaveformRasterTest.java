package com.github.curiousoddman.curious_tunes.ui.controller.custom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveformRasterTest {
    private static final int BG     = 0xFF3A4149;
    private static final int TOP    = 0xFFFF0000;
    private static final int BOTTOM = 0xFF0000FF;

    @Test
    void fullBarRunsFromTopColorToBottomColorAroundTheCentre() {
        int      w      = 4;
        int      h      = 20;
        int[]    pixels = new int[w * h];
        double[] bars   = {1, 1, 1, 1};
        WaveformRaster.render(pixels, w, h, bars, BG, TOP, BOTTOM);

        // Half height is 10; a full bar reaches 2 px from each edge.
        assertEquals(BG, pixels[0]);
        int firstBarRow = firstNonBackgroundRow(pixels, w, h, 0);
        int lastBarRow  = lastNonBackgroundRow(pixels, w, h, 0);
        assertEquals(TOP, pixels[firstBarRow * w]);
        assertEquals(BOTTOM, pixels[lastBarRow * w]);
        assertTrue(firstBarRow <= 2 && lastBarRow >= h - 3, firstBarRow + ".." + lastBarRow);
    }

    @Test
    void eachColumnShowsTheLoudestBarItCovers() {
        int      w      = 2;
        int      h      = 40;
        int[]    pixels = new int[w * h];
        double[] bars   = {0.1, 0.9, 0.1, 0.1};        // column 0 covers bars 0–1, column 1 covers bars 2–3
        WaveformRaster.render(pixels, w, h, bars, BG, TOP, BOTTOM);

        int tallColumn  = lastNonBackgroundRow(pixels, w, h, 0) - firstNonBackgroundRow(pixels, w, h, 0);
        int shortColumn = lastNonBackgroundRow(pixels, w, h, 1) - firstNonBackgroundRow(pixels, w, h, 1);
        assertTrue(tallColumn > 4 * shortColumn, tallColumn + " vs " + shortColumn);
    }

    @Test
    void silenceIsAThinLineAndFewerBarsThanColumnsStillFillEveryColumn() {
        int      w      = 10;
        int      h      = 40;
        int[]    pixels = new int[w * h];
        double[] bars   = {0, 0};
        WaveformRaster.render(pixels, w, h, bars, BG, TOP, BOTTOM);
        for (int x = 0; x < w; x++) {
            assertTrue(firstNonBackgroundRow(pixels, w, h, x) >= 0, "column " + x + " empty");
            assertTrue(lastNonBackgroundRow(pixels, w, h, x) - firstNonBackgroundRow(pixels, w, h, x) <= 2);
        }
    }

    private static int firstNonBackgroundRow(int[] pixels, int w, int h, int x) {
        for (int y = 0; y < h; y++) {
            if (pixels[y * w + x] != BG) {
                return y;
            }
        }
        return -1;
    }

    private static int lastNonBackgroundRow(int[] pixels, int w, int h, int x) {
        for (int y = h - 1; y >= 0; y--) {
            if (pixels[y * w + x] != BG) {
                return y;
            }
        }
        return -1;
    }
}
