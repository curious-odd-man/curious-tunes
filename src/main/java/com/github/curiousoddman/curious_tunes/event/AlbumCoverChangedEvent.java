package com.github.curiousoddman.curious_tunes.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * The cover of these albums was changed in the tag editor; the library views show the new cover.
 */
@Getter
public class AlbumCoverChangedEvent extends ApplicationEvent {
    private final Set<Integer> albumIds;

    public AlbumCoverChangedEvent(Object source, Set<Integer> albumIds) {
        super(source);
        this.albumIds = Set.copyOf(albumIds);
    }
}
