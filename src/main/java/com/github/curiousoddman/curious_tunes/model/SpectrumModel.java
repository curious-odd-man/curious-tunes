package com.github.curiousoddman.curious_tunes.model;

import org.springframework.stereotype.Component;

/**
 * The latest audio spectrum of the track being played, as reported by the media player
 * (magnitudes in dB, from the spectrum threshold up to 0). Views that animate the spectrum read it on every frame.
 * <p>
 * Used from the JavaFX application thread only.
 */
@Component
public class SpectrumModel {
    private float[] magnitudes = new float[0];
    private double  thresholdDb;
    private long    lastUpdateNanos;
    private int     version;

    /**
     * @param magnitudes  the player's array; it is reused by the player, so it is copied here
     * @param thresholdDb the spectrum threshold (the value of a silent band), e.g. -80
     */
    public void update(float[] magnitudes, double thresholdDb) {
        if (this.magnitudes.length != magnitudes.length) {
            this.magnitudes = new float[magnitudes.length];
        }
        System.arraycopy(magnitudes, 0, this.magnitudes, 0, magnitudes.length);
        this.thresholdDb = thresholdDb;
        this.lastUpdateNanos = System.nanoTime();
        version++;
    }

    /**
     * @return live array (not a copy); read only
     */
    public float[] getMagnitudes() {
        return magnitudes;
    }

    public double getThresholdDb() {
        return thresholdDb;
    }

    /**
     * When the player last reported a spectrum ({@link System#nanoTime()}), 0 if never. The player stops reporting
     * while paused or stopped, so an old value means there is nothing playing.
     */
    public long getLastUpdateNanos() {
        return lastUpdateNanos;
    }

    public int getVersion() {
        return version;
    }
}
