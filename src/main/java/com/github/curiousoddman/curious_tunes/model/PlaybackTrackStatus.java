package com.github.curiousoddman.curious_tunes.model;

import javafx.scene.media.MediaPlayer;

public enum PlaybackTrackStatus {
    NONE,
    LAUNCHING,
    PLAYING,
    PAUSED,
    ENDED;

    public static PlaybackTrackStatus map(MediaPlayer.Status sts) {
        return switch (sts) {
            case UNKNOWN -> NONE;
            case READY, STALLED -> LAUNCHING;
            case PAUSED, STOPPED -> PAUSED;
            case PLAYING -> PLAYING;
            case HALTED, DISPOSED -> ENDED;
        };
    }
}
