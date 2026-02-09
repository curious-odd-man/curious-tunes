package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.backend.MediaProvider;
import com.github.curiousoddman.curious_tunes.backend.tags.MetadataManager;
import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.BackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.EditTagsForTrackEvent;
import com.github.curiousoddman.curious_tunes.event.PlayPauseEvent;
import com.github.curiousoddman.curious_tunes.event.ShowArtistAlbums;
import com.github.curiousoddman.curious_tunes.event.player.PlayedThirdOfTrackEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import com.github.curiousoddman.curious_tunes.model.*;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistItemBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.RescanBundle;
import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistModel;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.backend.tags.FilesScanningService.LIBRARY_SCAN;
import static javafx.application.Platform.runLater;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryController implements Initializable {
    private final ApplicationEventPublisher eventPublisher;
    private final FxmlLoader fxmlLoader;
    private final DataAccess dataAccess;
    private final MetadataManager metadataManager;

    @FXML
    public Button buttonPlayPause;
    @FXML
    public Slider volumeControl;
    @FXML
    public ImageView currentTrackAlbumImage;
    @FXML
    public Label currentTrackName;
    @FXML
    public Label currentTrackAlbum;
    @FXML
    public Label currentTrackArtist;
    @FXML
    public ProgressBar currentTrackProgress;
    @FXML
    public Label timeSinceStart;
    @FXML
    public TextField searchField;
    @FXML
    public Label timeRemaining;
    @FXML
    public VBox artistList;
    @FXML
    public VBox artistAlbumsView;
    @FXML
    public Label artistTitle;
    @FXML
    public TabPane tabPane;
    @FXML
    public Tab albumsTab;
    @FXML
    public Tab historyTab;
    @FXML
    public Tab currentLyricsTab;
    @FXML
    public Tab editTagsTab;
    @FXML
    private AnchorPane playlistAnchorPane;

    private final List<LibraryArtistController> artistsControllers = new ArrayList<>();
    private final SimpleObjectProperty<PlaylistItem> currentTrackRecordObservable = new SimpleObjectProperty<>();
    private final PlaylistModel playlistModel;
    private final MediaProvider mediaProvider;

    private LibraryHistoryTabController libraryHistoryTabController;
    private LibraryLyricsTabController libraryLyricsTabController;
    private LibraryTagEditTabController libraryTagEditTabController;
    private ArtistSelectionModel artistSelectionModel;

    private boolean isPlaying = false;
    private boolean notifiedPlayedThird = false;
    private MediaPlayer player;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        artistSelectionModel = new ArtistSelectionModel(artistsControllers);
        LoadedFxml<LibraryPlaylistController> loadedFxml = fxmlLoader.load(
                FxmlView.LIBRARY_PLAYLIST,
                null
        );
        Parent parent = loadedFxml.parent();
        playlistAnchorPane.getChildren().add(parent);
        AnchorPane.setTopAnchor(parent, .0);
        AnchorPane.setBottomAnchor(parent, .0);
        AnchorPane.setLeftAnchor(parent, .0);
        AnchorPane.setRightAnchor(parent, .0);
        onLibraryDataUpdated();

        LoadedFxml<LibraryTagEditTabController> loaded = fxmlLoader.load(FxmlView.LIBRARY_TAB_TAGS_EDIT, null);
        libraryTagEditTabController = loaded.controller();
        editTagsTab.setContent(loaded.parent());
    }

    @EventListener
    @SneakyThrows
    public void onPlayPause(PlayPauseEvent playPauseEvent) {
        log.info("onPlayPause: Currently {}", isPlaying ? "playing" : "paused");
        if (!isPlaying) {
            Optional<PlaylistItem> optionalNext = playlistModel.getNextForPlayback();
            if (optionalNext.isEmpty()) {
                log.info("No items to play");
                return;
            }
            PlaylistItem playlistItem = optionalNext.get();
            eventPublisher.publishEvent(new PlayerStatusEvent(this, PlaybackTrackStatus.LAUNCHING, playlistItem));
            TrackRecord trackRecord = playlistItem.getTrackRecord();
            currentTrackRecordObservable.setValue(playlistItem);
            buttonPlayPause.setText("⏸");
            Media media = mediaProvider.getMedia(trackRecord);
            player = new MediaPlayer(media);
            notifiedPlayedThird = false;

            currentTrackName.setText(trackRecord.getTitle());
            currentTrackAlbum.setText(trackRecord.getFkAlbum().toString());
            currentTrackArtist.setText("");
            currentTrackProgress.setProgress(0);
            timeSinceStart.setText(String.valueOf(0));
            timeRemaining.setText(String.valueOf(trackRecord.getDuration()));

            // Providing functionality to time slider
            player.currentTimeProperty().addListener(ov -> {
                Duration currentTime = player.getCurrentTime();
                timeSinceStart.setText(TimeUtils.secondsToHumanTime((int) currentTime.toSeconds()));
                timeRemaining.setText(TimeUtils.secondsToHumanTime((int) (trackRecord.getDuration() - currentTime.toSeconds())));
                double progress = currentTime.toSeconds() / trackRecord.getDuration();
                // FIXME: This also works when you seek...
                if (progress > 0.3 && !notifiedPlayedThird) {
                    // TODO: Handle event
                    eventPublisher.publishEvent(new PlayedThirdOfTrackEvent(this, trackRecord));
                    notifiedPlayedThird = true;
                }
                currentTrackProgress.setProgress(progress);
            });

            volumeControl.valueProperty().addListener(ov -> {
                if (volumeControl.isPressed()) {
                    player.setVolume(volumeControl.getValue() / 100);
                }
            });

            loggingOnErrors(playlistItem);

            player.setOnEndOfMedia(() -> {
                isPlaying = false;
                eventPublisher.publishEvent(new PlayerStatusEvent(this, PlaybackTrackStatus.ENDED, playlistItem));
                eventPublisher.publishEvent(new PlayPauseEvent(this));
            });

            player.setVolume(volumeControl.getValue() / 100);
            player.play();
            isPlaying = true;
        } else {
            log.info("Pausing...");
            buttonPlayPause.setText("▶");
            player.pause();
            isPlaying = false;
        }
    }

    private void loggingOnErrors(PlaylistItem playlistItem) {
        player.onErrorProperty().addListener(observable -> log.error("Failed playback", player.getError()));
        player.onStalledProperty().addListener(observable -> log.error("Stalled {}", observable));

        player.statusProperty().addListener((observable, oldValue, newValue) -> {
            log.info("playback status: {} ", newValue);
            eventPublisher.publishEvent(new PlayerStatusEvent(this, PlaybackTrackStatus.map(newValue), playlistItem));
        });

        player.errorProperty().addListener((observable, oldValue, newValue) -> {
            log.error("error", newValue);
        });
    }

    @EventListener
    @SneakyThrows
    public void onShowArtistAlbumEvent(ShowArtistAlbums showArtistAlbums) {
        ArtistRecord artistRecord = showArtistAlbums.getArtistRecord();
        int artistId = artistRecord.getId();
        String artistName = artistRecord.getName();
        artistTitle.setText(artistName);
        artistAlbumsView.getChildren().remove(1, artistAlbumsView.getChildren().size());
        List<AlbumRecord> albums = dataAccess.getArtistAlbums(artistId);
        TrackSelectionModel trackSelectionModel = new TrackSelectionModel();
        for (AlbumRecord album : albums) {
            LoadedFxml<LibraryArtistAlbumController> loadedFxml = fxmlLoader.load(
                    FxmlView.LIBRARY_ARTIST_ALBUM,
                    new ArtistAlbumBundle(artistName, new AlbumInfo(artistRecord, album), trackSelectionModel)
            );
            artistAlbumsView.getChildren().add(loadedFxml.parent());
        }
    }

    @EventListener
    public void onBackgroundProcessEvent(BackgroundProcessEvent event) {
        runLater(() -> {
            if (event.getMaxProgress() > 0) {
                currentTrackProgress.setProgress((double) event.getProgress() / event.getMaxProgress());
            } else {
                currentTrackProgress.setProgress(0);
            }

            currentTrackName.setText(event.getProcessName());
            currentTrackAlbum.setText(event.getDescription());
            currentTrackName.setText("");
            timeSinceStart.setText(String.valueOf(event.getProgress()));
            timeRemaining.setText(String.valueOf(event.getMaxProgress() - event.getProgress()));

            if (event.getProcessName().equals(LIBRARY_SCAN)
                    && event.getEventType().isTerminal()) {
                onLibraryDataUpdated();
            }
        });
    }

    @EventListener
    public void onTagsUiUpdatedEvent(EditTagsForTrackEvent event) {
        tabPane.getSelectionModel().select(editTagsTab);
        TrackInfo trackInfo = event.getTrackInfo();
        TrackRecord trackRecord = trackInfo.getTrackRecord();
        libraryTagEditTabController.showTags(
                metadataManager.getMetadata(Path.of(trackRecord.getFileLocation())),
                trackInfo
        );
    }

    @SneakyThrows
    private void onLibraryDataUpdated() {
        artistsControllers.clear();
        artistList.getChildren().clear();
        for (ArtistRecord artist : dataAccess.getAllArtists()) {
            LoadedFxml<LibraryArtistController> loadedFxml = fxmlLoader.load(
                    FxmlView.LIBRARY_ARTIST_ITEM,
                    new ArtistItemBundle(artist, artistSelectionModel)
            );
            Parent parent = loadedFxml.parent();
            artistsControllers.add(loadedFxml.controller());
            artistList.getChildren().add(parent);
        }
    }

    @FXML
    public void onPlayPauseClick(ActionEvent actionEvent) {
        eventPublisher.publishEvent(new PlayPauseEvent(this));
    }

    @FXML
    @SneakyThrows
    public void onRescanMenuClicked(ActionEvent actionEvent) {
        Stage stage = new Stage();
        Parent root = fxmlLoader.load(FxmlView.RESCAN_MODAL, new RescanBundle("D:\\iTunes\\iTunes 1\\iTunes Media\\Music")).parent();
        stage.setScene(new Scene(root));
        stage.setTitle("Rescan library");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(buttonPlayPause.getScene().getWindow());
        stage.showAndWait();
    }

    @FXML
    public void onProgressClicked(MouseEvent mouseEvent) {
        double seekTo = (mouseEvent.getX() - currentTrackProgress.getLayoutX()) / currentTrackProgress.getWidth();

        TrackRecord currentTrack = playlistModel.getCurrentlyPlaying().get().getTrackRecord();
        Long duration = currentTrack.getDuration();
        log.info("Seek to {} : {}", seekTo, duration * seekTo);
        player.seek(Duration.seconds(duration * seekTo));
    }

    @FXML
    public void onTabSelectionChange(Event event) {
        if (historyTab != null && historyTab.isSelected()) {
            if (libraryHistoryTabController == null) {
                LoadedFxml<LibraryHistoryTabController> loaded = fxmlLoader.load(FxmlView.LIBRARY_TAB_HISTORY, null);
                libraryHistoryTabController = loaded.controller();
                historyTab.setContent(loaded.parent());
            } else {
                libraryHistoryTabController.renewStats();
            }
        } else if (currentLyricsTab != null && currentLyricsTab.isSelected()) {
            if (libraryLyricsTabController == null) {
                LoadedFxml<LibraryLyricsTabController> loaded = fxmlLoader.load(FxmlView.LIBRARY_TAB_LYRICS, null);
                libraryLyricsTabController = loaded.controller();
                currentLyricsTab.setContent(loaded.parent());
            }
            if (currentTrackRecordObservable.get() != null) {
                libraryLyricsTabController.showLyrics(currentTrackRecordObservable);
            }
        }
    }
}
