package com.github.curiousoddman.curious_tunes.app.preloader;

import javafx.application.Preloader;

/**
 * A line of text for the loading window ("Opening the library...").
 */
public record PreloaderStatus(String message) implements Preloader.PreloaderNotification {
}
