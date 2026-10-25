package com.github.curiousoddman.curious_tunes.model;

import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Waveform of the track being played. It is filled in live from the audio spectrum while the track plays,
 * and is shared by every view that draws a waveform (library and playback window).
 * <p>
 * Used from the JavaFX application thread only.
 */
@Component
public class WaveformModel {
    public static final int NUM_BARS = 2000;

    private final double[] bars = new double[NUM_BARS];
    private int version;

    /**
     * @return live array (not a copy), values are 0..1; read only
     */
    public double[] getBars() {
        return bars;
    }

    public void set(int index, double value) {
        bars[index] = value;
        version++;
    }

    public void reset() {
        Arrays.fill(bars, 0);
        version++;
    }

    /**
     * Changes every time the waveform data changes - lets a view skip redrawing when nothing changed.
     */
    public int getVersion() {
        return version;
    }
}
