package com.github.curiousoddman.curious_tunes.util;

import javafx.application.Platform;

import static javafx.application.Platform.runLater;

public class UiUtils {

    public static void runInUiThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            runLater(runnable);
        }
    }
}
