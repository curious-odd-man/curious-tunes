package com.github.curiousoddman.curious_tunes.util;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;

import java.util.Objects;

/**
 * The shared stylesheet (colors, type scale, icon colors) has to be on every Scene and every dialog.
 */
public final class UiStyles {
    private static final String GLOBAL_STYLESHEET = "/styles/global.css";

    private UiStyles() {
    }

    public static String globalStylesheet() {
        return Objects.requireNonNull(UiStyles.class.getResource(GLOBAL_STYLESHEET), "Missing " + GLOBAL_STYLESHEET)
                      .toExternalForm();
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().add(globalStylesheet());
    }

    public static void apply(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(globalStylesheet());
    }
}
