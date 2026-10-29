package com.github.curiousoddman.curious_tunes.event;

import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Open several tracks in the tag editor (batch edit): the fields the user changes are applied to all of them.
 */
@Getter
public class EditTagsForTracksEvent extends ApplicationEvent {
    private final List<TrackInfo> trackInfos;

    public EditTagsForTracksEvent(Object source, List<TrackInfo> trackInfos) {
        super(source);
        this.trackInfos = List.copyOf(trackInfos);
    }
}
