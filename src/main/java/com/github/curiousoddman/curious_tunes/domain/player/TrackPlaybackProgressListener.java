package com.github.curiousoddman.curious_tunes.domain.player;


import javafx.util.Duration;

public interface TrackPlaybackProgressListener {

    void onProgressUpdated(
            Duration currentTime,
            Duration trackDuration
    );

}
