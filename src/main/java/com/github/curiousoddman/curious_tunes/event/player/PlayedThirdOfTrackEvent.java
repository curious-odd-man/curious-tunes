package com.github.curiousoddman.curious_tunes.event.player;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PlayedThirdOfTrackEvent extends ApplicationEvent {
    private final TrackRecord trackRecord;
    private final int volume;

    public PlayedThirdOfTrackEvent(Object source,
                                   TrackRecord trackRecord,
                                   int volume) {
        super(source);
        this.trackRecord = trackRecord;
        this.volume = volume;
    }
}
