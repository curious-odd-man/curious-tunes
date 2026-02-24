package com.github.curiousoddman.curious_tunes.event;

import org.springframework.context.ApplicationEvent;

public class UserShutdownApplication extends ApplicationEvent {
    public UserShutdownApplication(Object source) {
        super(source);
    }
}
