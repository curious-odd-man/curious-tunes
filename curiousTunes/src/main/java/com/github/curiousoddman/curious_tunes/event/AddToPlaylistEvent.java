package com.github.curiousoddman.curious_tunes.event;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class AddToPlaylistEvent extends ApplicationEvent {
    private final List<TrackRecord> tracks;

    public AddToPlaylistEvent(Object source, List<TrackRecord> tracks) {
        super(source);
        this.tracks = tracks;
    }
}
