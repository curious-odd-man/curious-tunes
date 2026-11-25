package com.github.curiousoddman.curious_tunes.model.playlist;

import javafx.scene.Node;

/**
 * What the rows of the queue ask of the queue. Rows are cells of a virtualised list, reused for other tracks as
 * the list scrolls, so they report their position in the queue rather than themselves.
 */
public interface PlaylistDragHandler {
    void dragCompleted();

    /**
     * @param row   the row the drag starts on (for the drag image)
     * @param index its position in the queue
     */
    void dragInitiatedOn(Node row, int index);

    /**
     * @param index position in the queue of the row the drop landed on
     * @param below true when the pointer was in the lower half of the row: the track lands after the row, not before it
     */
    void dragDropped(int index, boolean below);

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
