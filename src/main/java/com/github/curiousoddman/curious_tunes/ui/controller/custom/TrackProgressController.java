package com.github.curiousoddman.curious_tunes.ui.controller.custom;

import javafx.scene.media.AudioSpectrumListener;

public interface TrackProgressController extends AudioSpectrumListener {
    int getAudioSpectrumThreshold();

    double getAudioSpectrumInterval();

    void startLoadingAnimation();

    void stopLoadingAnimation();
}
