package com.github.curiousoddman.curious_tunes.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ShowArtistAlbums extends ApplicationEvent {
    private final String artist;

    public ShowArtistAlbums(Object source, String artist) {
        super(source);
        this.artist = artist;
    }
}
