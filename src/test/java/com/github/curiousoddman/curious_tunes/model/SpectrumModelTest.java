package com.github.curiousoddman.curious_tunes.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectrumModelTest {

    @Test
    void frameAtGivesTheNewestFrameThatHadArrivedByThen() throws InterruptedException {
        SpectrumModel model  = new SpectrumModel();
        long          before = System.nanoTime();
        assertTrue(model.frameAt(before) == null);

        float[] magnitudes = {-10f};
        model.update(magnitudes, -80);
        Thread.sleep(5);
        long between = System.nanoTime();
        Thread.sleep(5);
        magnitudes[0] = -20f;           // the player reuses its array: the model must have copied it
        model.update(magnitudes, -80);

        assertTrue(model.frameAt(before) == null);
        assertEquals(-10, (int) model.frameAt(between)
                                     .magnitudes()[0]);
        assertEquals(-20, (int) model.frameAt(System.nanoTime())
                                     .magnitudes()[0]);
    }

    @Test
    void loudnessIsTheLoudestBandOnAZeroToOneScale() {
        assertEquals(0, SpectrumModel.loudness(new float[]{-80f, -80f}, -80), 1e-9);
        assertEquals(0.75, SpectrumModel.loudness(new float[]{-80f, -20f, -50f}, -80), 1e-9);
        assertEquals(1, SpectrumModel.loudness(new float[]{0f}, -80), 1e-9);
    }
}
