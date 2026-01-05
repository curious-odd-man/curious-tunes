package com.github.curiousoddman.curious_tunes.event.player;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PlayedThirdOfTrackEvent extends ApplicationEvent {
    private final TrackRecord trackRecord;

    public PlayedThirdOfTrackEvent(Object source, TrackRecord trackRecord) {
        super(source);
        this.trackRecord = trackRecord;
    }
}
