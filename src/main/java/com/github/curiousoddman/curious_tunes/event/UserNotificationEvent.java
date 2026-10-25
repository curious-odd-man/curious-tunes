package com.github.curiousoddman.curious_tunes.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * A short message for the user, shown for a few seconds in the status bar of the library window.
 * May be published from any thread.
 */
@Getter
public class UserNotificationEvent extends ApplicationEvent {
    private final String  message;
    private final boolean error;

    public UserNotificationEvent(Object source, String message, boolean error) {
        super(source);
        this.message = message;
        this.error = error;
    }

    public static UserNotificationEvent info(Object source, String message) {
        return new UserNotificationEvent(source, message, false);
    }

    public static UserNotificationEvent error(Object source, String message) {
        return new UserNotificationEvent(source, message, true);
    }
}
