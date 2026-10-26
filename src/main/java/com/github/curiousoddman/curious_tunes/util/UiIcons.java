package com.github.curiousoddman.curious_tunes.util;

import javafx.scene.control.Labeled;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Icons of the UI (Material Design Icons 2 through Ikonli). Literals are used in FXML as
 * {@code <FontIcon iconLiteral="mdi2p-play"/>}; the constants are for icons that change at run time.
 * Icon colors come from CSS (see global.css and playback-window.css).
 */
public final class UiIcons {
    public static final String PLAY            = "mdi2p-play";
    public static final String PAUSE           = "mdi2p-pause";
    public static final String SKIP_NEXT       = "mdi2s-skip-next";
    public static final String SKIP_PREVIOUS   = "mdi2s-skip-previous";
    public static final String VOLUME_HIGH     = "mdi2v-volume-high";
    public static final String VOLUME_MEDIUM   = "mdi2v-volume-medium";
    public static final String VOLUME_LOW      = "mdi2v-volume-low";
    public static final String VOLUME_OFF      = "mdi2v-volume-off";
    public static final String FULLSCREEN      = "mdi2f-fullscreen";
    public static final String FULLSCREEN_EXIT = "mdi2f-fullscreen-exit";
    public static final String MUSIC_NOTE      = "mdi2m-music-note";
    public static final String DISC            = "mdi2d-disc";

    private UiIcons() {
    }

    public static FontIcon icon(String literal) {
        return new FontIcon(literal);
    }

    public static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }

    /**
     * Shows the icon as the graphic of the control; an icon that is already there only changes its glyph.
     */
    public static void set(Labeled control, String literal) {
        if (control.getGraphic() instanceof FontIcon current) {
            current.setIconLiteral(literal);
        } else {
            control.setGraphic(icon(literal));
        }
    }

    /**
     * @param volume 0..100
     */
    public static String volume(double volume) {
        if (volume <= 0) {
            return VOLUME_OFF;
        }
        if (volume < 34) {
            return VOLUME_LOW;
        }
        if (volume < 67) {
            return VOLUME_MEDIUM;
        }
        return VOLUME_HIGH;
    }
}
