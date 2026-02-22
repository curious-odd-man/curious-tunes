package com.github.curiousoddman.curious_tunes.util.async;

import lombok.SneakyThrows;

public interface WaveformWriter {
    @SneakyThrows
    void append(double timestamp,
                double duration,
                float[] magnitudes,
                float[] phases,
                float db,
                double wi);

    void stop();
}
