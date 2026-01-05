package com.github.curiousoddman.curious_tunes.model;

import com.github.curiousoddman.curious_tunes.controller.LibraryArtistController;
import javafx.scene.control.SingleSelectionModel;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ArtistSelectionModel extends SingleSelectionModel<LibraryArtistController> {
    private final List<LibraryArtistController> artistList;

    @Override
    protected LibraryArtistController getModelItem(int index) {
        return artistList.get(index);
    }

    @Override
    protected int getItemCount() {
        return artistList.size();
    }

    public Optional<LibraryArtistController> getOptionalSelectedItem() {
        return Optional.ofNullable(getSelectedItem());
    }
}
