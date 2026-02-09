package com.github.curiousoddman.curious_tunes.event;

import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EditTagsForTrackEvent extends ApplicationEvent {
    private final TrackInfo trackInfo;

    public EditTagsForTrackEvent(Object source, TrackInfo trackInfo) {
        super(source);
        this.trackInfo = trackInfo;
    }
}
