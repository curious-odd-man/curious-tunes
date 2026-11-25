package com.github.curiousoddman.curious_tunes.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RescanLibraryEvent extends ApplicationEvent {
    private final String  path;
    /**
     * Read every file again, even those whose size and modification time haven't changed since the last scan
     * (e.g. after a change in how tags or lyrics are read).
     */
    private final boolean full;

    public RescanLibraryEvent(Object source, String path, boolean full) {
        super(source);
        this.path = path;
        this.full = full;
    }
}
