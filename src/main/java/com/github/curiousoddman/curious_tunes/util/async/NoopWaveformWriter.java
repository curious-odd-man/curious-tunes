package com.github.curiousoddman.curious_tunes.util.async;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "waveform-writer", havingValue = "noop", matchIfMissing = true)
public class NoopWaveformWriter implements WaveformWriter {
    @Override
    public void append(double timestamp, double duration, float[] magnitudes, float[] phases, float db, double wi) {

    }

    @Override
    public void stop() {

    }
}
