package com.github.curiousoddman.curious_tunes.ui.playback.calibration;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickTrackTest {

    @Test
    void writesAPlayableWavFile() throws Exception {
        Path file = Files.createTempFile("click-track-test", ".wav");
        try {
            ClickTrack.write(file);
            byte[] bytes = Files.readAllBytes(file);
            ByteBuffer header = ByteBuffer.wrap(bytes)
                                          .order(ByteOrder.LITTLE_ENDIAN);
            assertEquals("RIFF", new String(bytes, 0, 4));
            assertEquals("WAVE", new String(bytes, 8, 4));
            assertEquals(1, header.getShort(20));                         // PCM
            assertEquals(1, header.getShort(22));                         // mono
            assertEquals(ClickTrack.SAMPLE_RATE, header.getInt(24));
            assertEquals(16, header.getShort(34));
            int dataBytes = ClickTrack.SAMPLE_RATE * ClickTrack.SECONDS * 2;
            assertEquals(dataBytes, header.getInt(40));
            assertEquals(44 + dataBytes, bytes.length);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void oneClickPerSecondWithSilenceBetweenAndEveryFourthLouder() {
        short[] samples = ClickTrack.samples();
        double  normal  = 0;
        double  accent  = 0;
        for (int click = 0; click < ClickTrack.SECONDS; click++) {
            int    start = (int) Math.round(ClickTrack.clickTime(click) * ClickTrack.SAMPLE_RATE);
            double peak  = peak(samples, start, start + ClickTrack.SAMPLE_RATE / 20);
            assertTrue(peak > 0.25, "click " + click + " too quiet: " + peak);
            if (ClickTrack.isAccent(click)) {
                accent = peak;
            } else {
                normal = peak;
            }
            // Silent from 0.2 s after the click until the next one.
            double gap = peak(samples, start + ClickTrack.SAMPLE_RATE / 5, start + ClickTrack.SAMPLE_RATE - 10);
            assertTrue(gap < 0.001, "noise between clicks after click " + click + ": " + gap);
        }
        assertTrue(accent > normal * 1.4, "accent " + accent + " vs normal " + normal);
        // Nothing at the loop point, so looping never cuts a click.
        assertTrue(peak(samples, 0, ClickTrack.SAMPLE_RATE / 5) < 0.001);
    }

    private static double peak(short[] samples, int from, int to) {
        double peak = 0;
        for (int i = Math.max(0, from); i < Math.min(samples.length, to); i++) {
            peak = Math.max(peak, Math.abs(samples[i]) / (double) Short.MAX_VALUE);
        }
        return peak;
    }
}
