package com.github.curiousoddman.curious_tunes.ui.playback;

import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The visualiser delay: how long after the spectrum arrives the visualiser draws it, so it moves with what is heard
 * (the sound reaches the speakers later than the spectrum is measured). Set by the user in the calibration window,
 * saved in the user preferences, read live by the playback window's visualiser.
 * <p>
 * Used from the JavaFX application thread only.
 */
@Component
@RequiredArgsConstructor
public class VisualiserSync {
    private final UserPreferencesService userPreferencesService;

    private SimpleIntegerProperty delayMillis;

    public ReadOnlyIntegerProperty delayMillisProperty() {
        if (delayMillis == null) {
            delayMillis = new SimpleIntegerProperty(userPreferencesService.getVisualiserDelayMillis());
        }
        return delayMillis;
    }

    public int getDelayMillis() {
        return delayMillisProperty().get();
    }

    public void save(int millis) {
        int value = UserPreferencesService.clampVisualiserDelay(millis);
        userPreferencesService.saveVisualiserDelayMillis(value);
        delayMillisProperty();
        delayMillis.set(value);
    }
}
