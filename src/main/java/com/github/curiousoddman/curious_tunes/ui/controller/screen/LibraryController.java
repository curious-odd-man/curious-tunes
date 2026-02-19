package com.github.curiousoddman.curious_tunes.ui.controller.screen;

import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.domain.player.AudioPlayer;
import com.github.curiousoddman.curious_tunes.domain.tags.MetadataManager;
import com.github.curiousoddman.curious_tunes.event.BackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.EditTagsForTrackEvent;
import com.github.curiousoddman.curious_tunes.event.PlayPauseEvent;
import com.github.curiousoddman.curious_tunes.event.ShowArtistAlbums;
import com.github.curiousoddman.curious_tunes.model.ArtistSelectionModel;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.PlaybackState;
import com.github.curiousoddman.curious_tunes.model.TrackSelectionModel;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistItemBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.RescanBundle;
import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistModel;
import com.github.curiousoddman.curious_tunes.ui.controller.element.LibraryArtistAlbumController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.LibraryArtistController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.LibraryPlaylistController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryHistoryTabController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryLyricsTabController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryTagEditTabController;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
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
import java.util.*;
import java.util.function.Function;

import static com.github.curiousoddman.curious_tunes.domain.tags.FilesScanningService.LIBRARY_SCAN;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
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
    private final PlaylistModel playlistModel;
    private final AudioPlayer audioPlayer;

    private LibraryHistoryTabController libraryHistoryTabController;
    private LibraryLyricsTabController libraryLyricsTabController;
    private LibraryTagEditTabController libraryTagEditTabController;
    private ArtistSelectionModel artistSelectionModel;

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

        audioPlayer.getPlaybackStatusProperty()
                .addListener(observable -> {
                    PlaybackState status = audioPlayer.getPlaybackStatusProperty().get();
                    switch (status) {
                        case LAUNCHING -> {
                            buttonPlayPause.setText("⏸");
                            PlaylistItem playlistItem = audioPlayer.getPlaylistItemProperty().get();
                            currentTrackName.setText(playlistItem.getTitle());
                            currentTrackAlbum.setText(playlistItem.getAlbumName());
                            currentTrackArtist.setText(playlistItem.getArtistName());
                            currentTrackProgress.setProgress(0);
                            timeSinceStart.setText(String.valueOf(0));
                            timeRemaining.setText(String.valueOf(playlistItem.getDuration()));
                            libraryLyricsTabController.showLyrics(playlistItem);
                        }
                        case PLAYING -> buttonPlayPause.setText("⏸");
                        case STOPPED, PAUSED, ENDED -> buttonPlayPause.setText("▶");
                    }
                });
        audioPlayer.linkWithUi(
                volumeControl.valueProperty(),
                (currentDuration, totalDuration) -> {
                    timeSinceStart.setText(TimeUtils.secondsToHumanTime((int) currentDuration.toSeconds()));
                    timeRemaining.setText(TimeUtils.secondsToHumanTime((int) (totalDuration.toSeconds() - currentDuration.toSeconds())));
                    double progress = currentDuration.toSeconds() / totalDuration.toSeconds();
                    currentTrackProgress.setProgress(progress);
                }
        );

        LoadedFxml<LibraryLyricsTabController> lyricsTab = fxmlLoader.load(FxmlView.LIBRARY_TAB_LYRICS, null);
        libraryLyricsTabController = lyricsTab.controller();
        currentLyricsTab.setContent(lyricsTab.parent());
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
        List<YearAndLoadedFxml> loadedFxmlsByYear = new ArrayList<>();
        for (AlbumRecord album : albums) {
            List<TrackRecord> albumTracks = dataAccess.getAlbumTracks(album.getId());
            Integer yearFromTracks = getYearFromTracks(albumTracks);
            log.debug("For album {} year {}", album.getName(), yearFromTracks);
            loadedFxmlsByYear.add(
                    new YearAndLoadedFxml(
                            yearFromTracks,
                            fxmlLoader.load(
                                    FxmlView.LIBRARY_ARTIST_ALBUM,
                                    new ArtistAlbumBundle(artistName,
                                            new AlbumInfo(
                                                    artistRecord,
                                                    album,
                                                    yearFromTracks
                                            ),
                                            trackSelectionModel,
                                            albumTracks
                                    )
                            )));
        }
        List<Parent> rootElements = loadedFxmlsByYear
                .stream()
                .sorted(Comparator.nullsLast(Comparator.comparing(YearAndLoadedFxml::year)))
                .map(YearAndLoadedFxml::loadedFxml)
                .map(LoadedFxml::parent)
                .toList();
        artistAlbumsView.getChildren().addAll(rootElements);
    }

    record YearAndLoadedFxml(Integer year, LoadedFxml<LibraryArtistAlbumController> loadedFxml) {

    }

    private Integer getYearFromTracks(List<TrackRecord> albumTracks) {
        Map<Optional<Integer>, Long> countPerYear = albumTracks
                .stream()
                .map(TrackRecord::getReleaseDate)
                .map(text -> {
                    try {
                        return Optional.ofNullable(text).map(Integer::valueOf);
                    } catch (Exception e) {
                        log.warn("Album year cannot be extracted");
                        return Optional.<Integer>empty();
                    }
                })
                .filter(Optional::isPresent)
                .collect(groupingBy(Function.identity(), counting()));

        if (countPerYear.size() > 1) {
            log.warn("Different release dats found in album {}", countPerYear);
        }

        Integer year = null;
        long count = 0;
        for (Map.Entry<Optional<Integer>, Long> entry : countPerYear.entrySet()) {
            if (count < entry.getValue()) {
                count = entry.getValue();
                year = entry.getKey().orElse(null);
            }
        }
        return year;
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
        audioPlayer.seek(Duration.seconds(duration * seekTo));
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
        }
    }
}
