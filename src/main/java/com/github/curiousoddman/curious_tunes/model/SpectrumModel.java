package com.github.curiousoddman.curious_tunes.model;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Recent audio spectra of the track being played, as reported by the media player (magnitudes in dB, from the
 * spectrum threshold up to 0), each with the time it arrived.
 * <p>
 * The player reports a spectrum when the audio is decoded, which is ahead of the moment it is heard (the audio
 * output buffers it first). Views that animate the spectrum therefore ask for the frame that arrived a little
 * while ago ({@link #frameAt(long)}) rather than the newest one, so the picture lines up with the sound.
 * <p>
 * Used from the JavaFX application thread only.
 */
@Component
public class SpectrumModel {
    /**
     * Frames older than this are dropped; longer than any delay a view would use.
     */
    private static final long KEEP_NANOS = 2_000_000_000L;

    public record Frame(long arrivedNanos, float[] magnitudes) {
    }

    private final ArrayDeque<Frame> frames = new ArrayDeque<>();
    private       double            thresholdDb;

    /**
     * @param magnitudes  the player's array; it is reused by the player, so it is copied here
     * @param thresholdDb the spectrum threshold (the value of a silent band), e.g. -80
     */
    public void update(float[] magnitudes, double thresholdDb) {
        long now = System.nanoTime();
        frames.addLast(new Frame(now, magnitudes.clone()));
        this.thresholdDb = thresholdDb;
        while (!frames.isEmpty() && now - frames.peekFirst()
                                                .arrivedNanos() > KEEP_NANOS) {
            frames.removeFirst();
        }
    }

    /**
     * The frame to show at {@code nanos} ({@link System#nanoTime()} clock): the newest one that had arrived by then.
     *
     * @return null if none had arrived by then
     */
    public Frame frameAt(long nanos) {
        Frame result = null;
        for (Iterator<Frame> it = frames.iterator(); it.hasNext(); ) {
            Frame frame = it.next();
            if (frame.arrivedNanos() > nanos) {
                break;
            }
            result = frame;
        }
        return result;
    }

    public double getThresholdDb() {
        return thresholdDb;
    }

    /**
     * @return the loudest band of {@code magnitudes}, 0 (silent: at the threshold) to 1 (full scale: 0 dB)
     */
    public static double loudness(float[] magnitudes, double thresholdDb) {
        if (thresholdDb >= 0) {
            return 0;
        }
        double loudest = thresholdDb;
        for (float magnitude : magnitudes) {
            loudest = Math.max(loudest, magnitude);
        }
        return Math.max(0, Math.min(1, (loudest - thresholdDb) / -thresholdDb));
    }
}
