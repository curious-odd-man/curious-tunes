package com.github.curiousoddman.curious_tunes.util.async;


public interface WaveformWriter {
    void append(double timestamp,
                double duration,
                float[] magnitudes,
                float[] phases,
                float db,
                double wi);

    void stop();
}
