package com.github.curiousoddman.curious_tunes.ui.playback.calibration;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Generates the click track of the visualiser calibration: a WAV file (16-bit mono PCM) with one click per second,
 * every fourth click louder.
 * <p>
 * Each click is a few milliseconds of noise (it lights up every band, bass to treble) plus a short low tone, so the
 * whole visualiser jumps at once. The track is meant to be looped; its length is a whole number of seconds and the
 * clicks sit a quarter second into each second, away from the loop point.
 */
public final class ClickTrack {
    public static final int    SAMPLE_RATE      = 44_100;
    public static final int    SECONDS          = 8;
    public static final int    ACCENT_EVERY     = 4;
    /**
     * Position of the click in its second, so a gap at the loop point never cuts one.
     */
    public static final double CLICK_OFFSET_SEC = 0.25;

    static final double NOISE_SEC    = 0.006;
    static final double TONE_SEC     = 0.060;
    static final double TONE_HZ      = 80;
    static final double ACCENT_LEVEL = 0.9;
    static final double NORMAL_LEVEL = 0.5;

    private ClickTrack() {
    }

    /**
     * Writes the track to {@code file}, replacing it.
     */
    public static void write(Path file) throws IOException {
        short[] samples = samples();
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(wavHeader(samples.length));
            ByteBuffer data = ByteBuffer.allocate(samples.length * 2)
                                        .order(ByteOrder.LITTLE_ENDIAN);
            for (short sample : samples) {
                data.putShort(sample);
            }
            out.write(data.array());
        }
    }

    /**
     * @return the time of click {@code index} (0-based) from the start of the track, in seconds
     */
    public static double clickTime(int index) {
        return index + CLICK_OFFSET_SEC;
    }

    public static boolean isAccent(int index) {
        return index % ACCENT_EVERY == 0;
    }

    static short[] samples() {
        double[] signal = new double[SAMPLE_RATE * SECONDS];
        Random   noise  = new Random(42);      // the same track every time
        for (int click = 0; click < SECONDS; click++) {
            double level = isAccent(click) ? ACCENT_LEVEL : NORMAL_LEVEL;
            int    start = (int) Math.round(clickTime(click) * SAMPLE_RATE);
            int    tone  = (int) (TONE_SEC * SAMPLE_RATE);
            int    burst = (int) (NOISE_SEC * SAMPLE_RATE);
            for (int i = 0; i < tone && start + i < signal.length; i++) {
                double t     = (double) i / SAMPLE_RATE;
                double value = 0.6 * Math.sin(2 * Math.PI * TONE_HZ * t) * Math.exp(-t / (TONE_SEC / 4));
                if (i < burst) {
                    value += 0.6 * (noise.nextDouble() * 2 - 1) * Math.exp(-t / (NOISE_SEC / 3));
                }
                signal[start + i] += level * value;
            }
        }
        short[] result = new short[signal.length];
        for (int i = 0; i < signal.length; i++) {
            double clipped = Math.max(-1, Math.min(1, signal[i]));
            result[i] = (short) Math.round(clipped * Short.MAX_VALUE);
        }
        return result;
    }

    private static byte[] wavHeader(int sampleCount) {
        int        dataBytes = sampleCount * 2;
        ByteBuffer header    = ByteBuffer.allocate(44)
                                         .order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{'R', 'I', 'F', 'F'})
              .putInt(36 + dataBytes)
              .put(new byte[]{'W', 'A', 'V', 'E'})
              .put(new byte[]{'f', 'm', 't', ' '})
              .putInt(16)                       // size of the format chunk
              .putShort((short) 1)              // PCM
              .putShort((short) 1)              // mono
              .putInt(SAMPLE_RATE)
              .putInt(SAMPLE_RATE * 2)          // bytes per second
              .putShort((short) 2)              // bytes per sample frame
              .putShort((short) 16)             // bits per sample
              .put(new byte[]{'d', 'a', 't', 'a'})
              .putInt(dataBytes);
        return header.array();
    }
}
