package com.github.curiousoddman.curious_tunes.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Slider;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Colors the part of a slider's track that is left of the thumb (the standard slider does not).
 */
public final class SliderFill {
    private SliderFill() {
    }

    /**
     * @param filledColor CSS color of the part left of the thumb (a color or a looked-up color such as -ct-accent)
     * @param emptyColor  CSS color of the rest
     * @return a runnable that redraws the track, for when a color changes
     */
    public static Runnable install(Slider slider, Supplier<String> filledColor, Supplier<String> emptyColor) {
        Runnable update = () -> {
            Node track = slider.lookup(".track");
            if (track == null) {
                return;
            }
            double range   = slider.getMax() - slider.getMin();
            double percent = range <= 0 ? 0 : (slider.getValue() - slider.getMin()) / range * 100;
            track.setStyle(String.format(Locale.ROOT,
                    "-fx-background-color: linear-gradient(to right, %s %.1f%%, %s %.1f%%); "
                            + "-fx-background-insets: 0; -fx-background-radius: 3;",
                    filledColor.get(), percent, emptyColor.get(), percent));
        };
        slider.valueProperty().addListener(o -> update.run());
        slider.skinProperty().addListener(o -> Platform.runLater(update));
        Platform.runLater(update);
        return update;
    }
}
