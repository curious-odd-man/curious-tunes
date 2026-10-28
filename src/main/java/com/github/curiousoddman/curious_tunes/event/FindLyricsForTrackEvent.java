package com.github.curiousoddman.curious_tunes.event;

import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Open the track in the tag editor and look for its lyrics (the user reviews them before saving).
 */
@Getter
public class FindLyricsForTrackEvent extends ApplicationEvent {
    private final TrackInfo trackInfo;

    public FindLyricsForTrackEvent(Object source, TrackInfo trackInfo) {
        super(source);
        this.trackInfo = trackInfo;
    }
}
