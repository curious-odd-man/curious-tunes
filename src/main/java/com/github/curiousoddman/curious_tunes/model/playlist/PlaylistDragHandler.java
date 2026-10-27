package com.github.curiousoddman.curious_tunes.model.playlist;

import javafx.scene.layout.AnchorPane;

public interface PlaylistDragHandler {
    void dragCompleted();

    void dragInitiatedOn(AnchorPane pane);

    /**
     * @param below true when the pointer was in the lower half of the row: the track lands after the row, not before it
     */
    void dragDropped(AnchorPane pane, boolean below);

    /**
     * The track was dropped on the free space under the last row: it moves to the end of the queue.
     */
    void dragDroppedAtEnd();

    /**
     * Removes the item from the queue (context menu action).
     */
    void removeItem(PlaylistItem playlistItem);

    /**
     * Puts the item right after the track that is playing (context menu action).
     */
    void moveToNext(PlaylistItem playlistItem);

    /**
     * Puts the item at the end of the queue (context menu action).
     */
    void moveToEnd(PlaylistItem playlistItem);

    /**
     * Shows the track in the library: its artist and album (context menu action).
     */
    void showInLibrary(PlaylistItem playlistItem);
}
