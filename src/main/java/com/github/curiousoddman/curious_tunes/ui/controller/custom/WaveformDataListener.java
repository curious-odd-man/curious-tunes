package com.github.curiousoddman.curious_tunes.ui.controller.custom;

import javafx.scene.media.AudioSpectrumListener;

public interface WaveformDataListener extends AudioSpectrumListener {
    int getAudioSpectrumThreshold();

    double getAudioSpectrumInterval();
}
