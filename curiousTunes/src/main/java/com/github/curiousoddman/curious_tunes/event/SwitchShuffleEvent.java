package com.github.curiousoddman.curious_tunes.event;

import org.springframework.context.ApplicationEvent;

public class SwitchShuffleEvent extends ApplicationEvent {
    public SwitchShuffleEvent(Object source) {
        super(source);
    }
}
