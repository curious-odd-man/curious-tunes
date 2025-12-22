package com.github.curiousoddman.curious_tunes.event;

import org.springframework.context.ApplicationEvent;

public class InterruptBackgroundProcessEvent extends ApplicationEvent {
    public InterruptBackgroundProcessEvent(Object source) {
        super(source);
    }
}
