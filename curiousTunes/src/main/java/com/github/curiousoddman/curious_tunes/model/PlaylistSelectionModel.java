package com.github.curiousoddman.curious_tunes.model;

import com.github.curiousoddman.curious_tunes.controller.PlaylistItemController;
import javafx.scene.control.SingleSelectionModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class PlaylistSelectionModel extends SingleSelectionModel<PlaylistItemController> {
    @Getter
    private final List<PlaylistItemController> playlistItems;

    @Override
    protected PlaylistItemController getModelItem(int index) {
        return playlistItems.get(index);
    }

    @Override
    protected int getItemCount() {
        return playlistItems.size();
    }

    public Optional<PlaylistItemController> getOptionalSelectedItem() {
        return Optional.ofNullable(getSelectedItem());
    }

    public void clear() {
        if (!isEmpty()) {
            clearSelection();
        }
        playlistItems.clear();
    }
}
