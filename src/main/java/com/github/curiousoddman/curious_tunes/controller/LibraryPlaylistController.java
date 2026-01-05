package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.PlaylistModel;
import com.github.curiousoddman.curious_tunes.model.bundle.PlaylistItemResourceBundle;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;

import static javafx.application.Platform.runLater;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryPlaylistController implements Initializable {
    private final Map<PlaylistItem, PlaylistItemController> playlistItemControllers = new HashMap<>();

    private final PlaylistModel playlistModel;
    private final FxmlLoader fxmlLoader;
    private final DataAccess dataAccess;

    @FXML
    private VBox playlistVbox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @EventListener
    public void onAddToPlaylist(AddToPlaylistEvent addToPlaylistEvent) {
        playlistModel.addItems(addToPlaylistEvent);
        redrawPlaylist();
    }

    @EventListener
    public void onPlayerStatusEvent(PlayerStatusEvent playerStatusEvent) {
        playlistModel.updateStatuses(playerStatusEvent);
        PlaylistItem playlistItem = playerStatusEvent.getPlaylistItem();
        PlaylistItemController playlistItemController = playlistItemControllers.get(playlistItem);
        playlistItemController.updateStyle();
    }

    public void redrawPlaylist() {
        List<PlaylistItem> playlistItems = playlistModel.getPlaylistItems();
        Map<PlaylistItem, Map.Entry<AlbumRecord, ArtistRecord>> tracksInfo = dataAccess.getArtistAlbumForTracks(playlistItems);
        playlistItemControllers.clear();

        runLater(() -> {
            ObservableList<Node> playlistNodes = playlistVbox.getChildren();
            playlistNodes.clear();

            if (playlistItems.isEmpty()) {
                return;
            }

            for (Map.Entry<PlaylistItem, Map.Entry<AlbumRecord, ArtistRecord>> entry : tracksInfo.entrySet()) {
                PlaylistItem playlistItem = entry.getKey();
                Map.Entry<AlbumRecord, ArtistRecord> albumArtist = entry.getValue();
                AlbumRecord albumRecord = albumArtist.getKey();
                ArtistRecord artistRecord = albumArtist.getValue();
                LoadedFxml<PlaylistItemController> loadedFxml = fxmlLoader.load(
                        FxmlView.PLAYLIST_ITEM,
                        new PlaylistItemResourceBundle(
                                artistRecord.getName(),
                                albumRecord,
                                playlistItem,
                                playlistModel
                        )
                );
                PlaylistItemController playlistItemController = loadedFxml.controller();
                playlistItemControllers.put(playlistItem, playlistItemController);
                Parent parent = loadedFxml.parent();
                playlistItemController.updateStyle();
                playlistNodes.add(parent);
            }
        });
    }

    @FXML
    public void onUpButtonClick(ActionEvent actionEvent) {
        OptionalInt movedIndex = playlistModel.moveSelectedUp();
        movedIndex.ifPresent(i -> swap(playlistVbox.getChildren(), i - 1, i));
    }

    @FXML
    public void onDownButtonClick(ActionEvent actionEvent) {
        OptionalInt movedIndex = playlistModel.moveSelectedDown();
        movedIndex.ifPresent(i -> swap(playlistVbox.getChildren(), i + 1, i));
    }

    private void swap(ObservableList<Node> children, int i1, int i2) {
        Label fakeLabel = new Label();      // Cannot set null
        Node removed1 = children.set(i1, fakeLabel);
        Node removed2 = children.set(i2, removed1);
        children.set(i1, removed2);
    }

    @FXML
    public void onDeleteClick(ActionEvent actionEvent) {
        PlaylistItem item = playlistModel.getSelected();
        OptionalInt deletedIndex = playlistModel.deleteSelected();
        deletedIndex.ifPresent(i -> {
            playlistVbox.getChildren().remove(i);
            playlistItemControllers.remove(item);
        });
    }

    @FXML
    public void onShuffleClicked(ActionEvent actionEvent) {
        playlistModel.shuffle();
        redrawPlaylist();       // TODO: Very inefficient
    }

    @FXML
    public void onClearPlaylistClicked(ActionEvent actionEvent) {
        playlistModel.clear();
        redrawPlaylist();
    }
}
