package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.backend.player.CurrentPlaylistService;
import com.github.curiousoddman.curious_tunes.backend.player.PlaylistItem;
import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.config.StageManager;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.PlaylistUpdatedEvent;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.PlaylistSelectionModel;
import com.github.curiousoddman.curious_tunes.model.bundle.PlaylistItemResourceBundle;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static javafx.application.Platform.runLater;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryPlaylistController implements Initializable {
    private final StageManager stageManager;
    private final ApplicationEventPublisher eventPublisher;
    private final FxmlLoader fxmlLoader;
    private final DataAccess dataAccess;

    @FXML
    private VBox playlistVbox;

    private final CurrentPlaylistService currentPlaylistService;
    private PlaylistSelectionModel playlistSelectionModel;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        playlistSelectionModel = new PlaylistSelectionModel(new ArrayList<>());
    }

    @EventListener
    public void onPlaylistUpdatedEvent(PlaylistUpdatedEvent playlistUpdatedEvent) {
        runLater(() -> {
            playlistSelectionModel.clear();
            List<PlaylistItem> playlistItems = currentPlaylistService.getPlaylistItems();
            ObservableList<Node> playlistNodes = playlistVbox.getChildren();
            playlistNodes.clear();
            Map<PlaylistItem, Map.Entry<AlbumRecord, ArtistRecord>> tracksInfo = dataAccess.getArtistAlbumForTracks(playlistItems);

            for (Map.Entry<PlaylistItem, Map.Entry<AlbumRecord, ArtistRecord>> entry : tracksInfo.entrySet()) {
                PlaylistItem playlistItem = entry.getKey();
                TrackRecord trackRecord = playlistItem.getTrackRecord();
                Map.Entry<AlbumRecord, ArtistRecord> albumArtist = entry.getValue();
                AlbumRecord albumRecord = albumArtist.getKey();
                ArtistRecord artistRecord = albumArtist.getValue();
                LoadedFxml<PlaylistItemController> loadedFxml = fxmlLoader.load(
                        FxmlView.PLAYLIST_ITEM,
                        new PlaylistItemResourceBundle(
                                artistRecord.getName(),
                                albumRecord,
                                trackRecord,
                                playlistSelectionModel
                        )
                );
                playlistSelectionModel.getPlaylistItems().add(loadedFxml.controller());
                Parent parent = loadedFxml.parent();
                applyStyle(playlistItem, parent);
                playlistNodes.add(parent);
            }
        });
    }

    private static void applyStyle(PlaylistItem playlistItem, Parent parent) {
        String styleClass = switch (playlistItem.getPlaylistItemStatus()) {
            case PLAYING -> "playlist-item-playing";
            case SKIPPED, QUEUED, FILE_NOT_FOUND, TO_BE_SKIPPED -> null;
            case PLAYED -> "playlist-item-played";
        };
        if (styleClass != null) {
            parent.getStyleClass().add(styleClass);
        }
    }

    public void onUpButtonClick(ActionEvent actionEvent) {

    }

    public void onDownButtonClick(ActionEvent actionEvent) {

    }

    public void onDeleteClick(ActionEvent actionEvent) {

    }

    public void onShuffleClicked(ActionEvent actionEvent) {

    }
}
