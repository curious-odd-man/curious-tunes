package com.github.curiousoddman.curious_tunes.ui.playback;

import java.util.Arrays;

/**
 * Turns the player's spectrum into bar heights for the visualiser, with no JavaFX dependency so it can be tested.
 * <p>
 * Bars go from bass (left) to treble (right). The player's bands are linear in frequency, so low bars get one band
 * each and higher bars group more and more bands (roughly logarithmic), which keeps the bass from being squeezed
 * into the first bar or two.
 * <p>
 * Motion: a bar rises quickly to a louder value and falls back slowly; its peak cap holds for a moment and then
 * drops. When no new spectrum arrives (paused, stopped) everything falls to zero.
 */
public class SpectrumBars {
    /**
     * Highest frequency shown, as a share of the bands (the top bands are mostly empty in typical music).
     */
    static final double MAX_BAND_SHARE    = 0.75;
    /**
     * Extra gain across the bars from left (none) to right (this share), so the quieter treble still moves.
     * Proportional, so a silent band stays at zero.
     */
    static final double TREBLE_GAIN       = 0.2;
    static final double ATTACK_PER_SEC    = 30;     // rising: how fast the gap to a louder value closes
    static final double RELEASE_PER_SEC   = 1.6;    // falling: height units per second
    static final double PEAK_HOLD_SEC     = 0.35;
    static final double PEAK_FALL_PER_SEC = 0.9;

    private final int      barCount;
    private final double[] targets;
    private final double[] levels;
    private final double[] peaks;
    private final double[] peakAge;

    private int[] firstBand      = new int[0];
    private int[] endBand        = new int[0];
    private int   rangesForBands = -1;

    public SpectrumBars(int barCount) {
        this.barCount = barCount;
        targets = new double[barCount];
        levels = new double[barCount];
        peaks = new double[barCount];
        peakAge = new double[barCount];
    }

    public int getBarCount() {
        return barCount;
    }

    /**
     * Sets the heights the bars move towards from a new spectrum.
     *
     * @param magnitudes  dB per band, from {@code thresholdDb} (silent) to 0
     * @param thresholdDb the value of a silent band, negative (e.g. -80)
     */
    public void setSpectrum(float[] magnitudes, double thresholdDb) {
        if (magnitudes.length == 0 || thresholdDb >= 0) {
            silence();
            return;
        }
        if (rangesForBands != magnitudes.length) {
            int[][] ranges = bandRanges(barCount, magnitudes.length);
            firstBand = ranges[0];
            endBand = ranges[1];
            rangesForBands = magnitudes.length;
        }
        double range = -thresholdDb;
        for (int bar = 0; bar < barCount; bar++) {
            double loudest = thresholdDb;
            for (int band = firstBand[bar]; band < endBand[bar]; band++) {
                loudest = Math.max(loudest, magnitudes[band]);
            }
            double gain = 1 + TREBLE_GAIN * bar / Math.max(1, barCount - 1);
            targets[bar] = clamp((loudest - thresholdDb) / range * gain);
        }
    }

    /**
     * Nothing is playing: the bars fall to zero.
     */
    public void silence() {
        Arrays.fill(targets, 0);
    }

    /**
     * Moves the bars and peaks on by {@code seconds}.
     */
    public void step(double seconds) {
        double dt     = Math.clamp(seconds, 0, 0.1);       // a long pause between frames must not jump
        double attack = 1 - Math.exp(-ATTACK_PER_SEC * dt);
        for (int i = 0; i < barCount; i++) {
            double target = targets[i];
            if (target > levels[i]) {
                levels[i] += (target - levels[i]) * attack;
            } else {
                levels[i] = Math.max(target, levels[i] - RELEASE_PER_SEC * dt);
            }

            if (levels[i] >= peaks[i]) {
                peaks[i] = levels[i];
                peakAge[i] = 0;
            } else {
                peakAge[i] += dt;
                if (peakAge[i] > PEAK_HOLD_SEC) {
                    peaks[i] = Math.max(levels[i], peaks[i] - PEAK_FALL_PER_SEC * dt);
                }
            }
        }
    }

    /**
     * @return true when every bar and peak is at zero and nothing more will move until a new spectrum arrives
     */
    public boolean isIdle() {
        for (int i = 0; i < barCount; i++) {
            if (levels[i] > 0 || peaks[i] > 0 || targets[i] > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return live array of bar heights 0..1; read only
     */
    public double[] getLevels() {
        return levels;
    }

    /**
     * @return live array of peak cap heights 0..1; read only
     */
    public double[] getPeaks() {
        return peaks;
    }

    /**
     * The bands that make up each bar: bar {@code i} covers bands {@code [0][i]} (inclusive) to {@code [1][i]}
     * (exclusive). Every bar has at least one band. With more bands than bars the ranges are roughly log-spaced and
     * follow each other without gaps; with fewer bands than bars, neighbouring bars share bands.
     */
    static int[][] bandRanges(int barCount, int bandCount) {
        int[] first   = new int[barCount];
        int[] end     = new int[barCount];
        int   maxBand = Math.clamp(bandCount, 1, (int) Math.round(bandCount * MAX_BAND_SHARE));
        if (maxBand <= barCount) {
            for (int i = 0; i < barCount; i++) {
                first[i] = Math.min(maxBand - 1, i * maxBand / barCount);
                end[i] = first[i] + 1;
            }
            return new int[][]{first, end};
        }
        // Log-spaced edges, but never less than one band per bar: where the log spacing would give a bar less
        // than one band, it takes one, and the rest of the range is spread over the bars still to come.
        int previous = 0;
        for (int i = 0; i < barCount; i++) {
            double logEdge = Math.pow(maxBand, (double) (i + 1) / barCount);
            int    edge    = Math.max(previous + 1, (int) Math.round(logEdge));
            edge = Math.min(edge, maxBand - (barCount - 1 - i));       // leave one band for each bar still to come
            first[i] = previous;
            end[i] = edge;
            previous = edge;
        }
        end[barCount - 1] = maxBand;
        return new int[][]{first, end};
    }

    private static double clamp(double value) {
        return Math.clamp(value, 0, 1);
    }
}
