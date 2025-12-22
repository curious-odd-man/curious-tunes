package com.github.curiousoddman.curious_tunes.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PlayNextEvent extends ApplicationEvent {
    private final boolean shuffleOn;

    public PlayNextEvent(Object source, boolean shuffleOn) {
        super(source);
        this.shuffleOn = shuffleOn;
    }
}
