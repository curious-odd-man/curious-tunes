package com.github.curiousoddman.curious_tunes.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Component;

/**
 * Holds the id of the track that is playing right now (null when nothing plays).
 * Library rows observe it to show a "now playing" marker. Updated on the FX thread.
 */
@Component
public class NowPlayingMarker {
    private final ObjectProperty<Integer> trackId = new SimpleObjectProperty<>();

    public ObjectProperty<Integer> trackIdProperty() {
        return trackId;
    }
}
