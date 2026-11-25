package com.github.curiousoddman.curious_tunes.ui.controller.custom;

import java.util.Arrays;

/**
 * Draws the waveform bar's picture into an ARGB pixel array: one centred bar per pixel column, filled with a
 * vertical gradient (top color at the bar's top, bottom color at its bottom), on a background.
 * <p>
 * Pure Java, no JavaFX, so it can be tested. It replaces drawing pixel by pixel through a {@code PixelWriter} with a
 * new {@code Color} object per pixel (doc/performance-review.md, item 4.1): the whole image is computed in one array
 * and written to the screen image in one call.
 * <p>
 * There are usually more bars ({@code bars.length}, 2000) than pixel columns, so each column shows the loudest of the
 * bars it covers. That way short peaks stay visible instead of depending on which bar happened to be drawn last.
 */
public final class WaveformRaster {
    /**
     * Bars never shrink below this share of the half height, so silence still shows as a thin line.
     */
    static final double MIN_AMPLITUDE = 0.03;

    private WaveformRaster() {
    }

    /**
     * @param pixels at least {@code width * height} entries, row by row; overwritten
     * @param bars   amplitudes 0..1
     * @param bg     background, ARGB
     * @param top    bar color at its top, ARGB (opaque)
     * @param bottom bar color at its bottom, ARGB (opaque)
     */
    public static void render(int[] pixels, int width, int height, double[] bars, int bg, int top, int bottom) {
        if (width <= 0 || height <= 0) {
            return;
        }
        Arrays.fill(pixels, 0, width * height, bg);
        if (bars.length == 0) {
            return;
        }
        double centerY  = height / 2.0;
        int    topR     = (top >> 16) & 0xFF;
        int    topG     = (top >> 8) & 0xFF;
        int    topB     = top & 0xFF;
        int    bottomR  = (bottom >> 16) & 0xFF;
        int    bottomG  = (bottom >> 8) & 0xFF;
        int    bottomB  = bottom & 0xFF;

        for (int x = 0; x < width; x++) {
            int    first = (int) ((long) x * bars.length / width);
            int    end   = Math.max(first + 1, (int) ((long) (x + 1) * bars.length / width));
            double amp   = 0;
            for (int b = first; b < end && b < bars.length; b++) {
                amp = Math.max(amp, bars[b]);
            }
            amp = Math.clamp(amp, MIN_AMPLITUDE, 1);

            double halfH = amp * (centerY - 2);
            int    yTop  = Math.max(0, (int) (centerY - halfH));
            int    yBot  = Math.min(height - 1, (int) (centerY + halfH));
            int    span  = yBot - yTop;
            for (int y = yTop; y <= yBot; y++) {
                double t = span > 0 ? (double) (y - yTop) / span : 0;
                pixels[y * width + x] = 0xFF000000
                        | lerp(topR, bottomR, t) << 16
                        | lerp(topG, bottomG, t) << 8
                        | lerp(topB, bottomB, t);
            }
        }
    }

    private static int lerp(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }
}
