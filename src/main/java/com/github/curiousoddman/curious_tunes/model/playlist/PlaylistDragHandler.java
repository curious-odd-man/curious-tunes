package com.github.curiousoddman.curious_tunes.model.playlist;

import javafx.scene.layout.AnchorPane;

public interface PlaylistDragHandler {
    void dragCompleted();

    void dragInitiatedOn(AnchorPane pane);

    void dragDropped(AnchorPane pane);
}
