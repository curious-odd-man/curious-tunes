package com.github.curiousoddman.curious_tunes.model.playlist;

import javafx.scene.control.SingleSelectionModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class PlaylistSelectionModel extends SingleSelectionModel<PlaylistItem> {
    @Getter
    private final List<PlaylistItem> playlistItems;

    @Override
    protected PlaylistItem getModelItem(int index) {
        if (index < 0 || index >= playlistItems.size()) {
            return null;
        }
        return playlistItems.get(index);
    }

    @Override
    protected int getItemCount() {
        return playlistItems.size();
    }

    public Optional<PlaylistItem> getOptionalSelectedItem() {
        return Optional.ofNullable(getSelectedItem());
    }

    public void clear() {
        if (!isEmpty()) {
            clearSelection();
        }
        playlistItems.clear();
    }
}
