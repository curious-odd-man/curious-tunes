package com.github.curiousoddman.curious_tunes.util;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.List;

/**
 * Images packaged with the app, loaded by URL: nothing to close, and a missing file fails with its name rather than
 * a {@code NullPointerException} inside {@code Image}.
 */
public final class AppResources {
    private AppResources() {
    }

    /**
     * @return the window icons, smallest first
     */
    public static List<Image> appIcons() {
        return List.of(image("/icons/app-icon-16.png"), image("/icons/app-icon-32.png"),
                image("/icons/app-icon-64.png"), image("/icons/app-icon-128.png"));
    }

    /**
     * @param path absolute class path location, e.g. {@code /img/no-image.png}
     */
    public static Image image(String path) {
        URL url = AppResources.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing resource " + path);
        }
        return new Image(url.toExternalForm());
    }
}
