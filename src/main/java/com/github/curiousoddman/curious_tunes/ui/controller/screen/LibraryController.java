package com.github.curiousoddman.curious_tunes.ui.controller.screen;

import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.domain.player.AudioPlayer;
import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
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
import com.github.curiousoddman.curious_tunes.ui.controller.custom.ProgressCanvasController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.LibraryArtistAlbumController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.LibraryArtistController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.LibraryPlaylistController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryHistoryTabController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryLyricsTabController;
import com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryTagEditTabController;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import com.github.curiousoddman.curious_tunes.util.async.DelayedAction;
import javafx.beans.InvalidationListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.github.curiousoddman.curious_tunes.domain.tags.FilesScanningService.LIBRARY_SCAN;
import static com.sun.javafx.util.Utils.runOnFxThread;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryController implements Initializable {
    private final ApplicationEventPublisher eventPublisher;
    private final FxmlLoader fxmlLoader;
    private final DataAccess dataAccess;

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
    public Canvas currentTrackProgress;
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
    public StackPane canvasPane;
    public SplitPane librarySplitPane;
    @FXML
    private AnchorPane playlistAnchorPane;

    private final List<LibraryArtistController> artistsControllers = new ArrayList<>();
    private final ProgressCanvasController progressCanvasController;
    private final PlaylistModel playlistModel;
    private final AudioPlayer audioPlayer;
    private final UserPreferencesService userPreferencesService;

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
        progressCanvasController.init(canvasPane, currentTrackProgress);

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
                            timeSinceStart.setText(String.valueOf(0));
                            timeRemaining.setText('-' + String.valueOf(playlistItem.getDuration()));
                            libraryLyricsTabController.showLyrics(playlistItem);
                            progressCanvasController.startProgress();
                        }
                        case PLAYING -> buttonPlayPause.setText("⏸");
                        case STOPPED, PAUSED, ENDED -> buttonPlayPause.setText("▶");
                    }
                });
        audioPlayer.linkWithUi(
                volumeControl.valueProperty(),
                (currentDuration, totalDuration) -> {
                    timeSinceStart.setText(TimeUtils.secondsToHumanTime((int) currentDuration.toSeconds()));
                    timeRemaining.setText('-' + TimeUtils.secondsToHumanTime((int) (totalDuration.toSeconds() - currentDuration.toSeconds())));
                    progressCanvasController.setProgress(currentDuration, totalDuration);
                },
                progressCanvasController
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
        Thread t = new Thread(() -> {
            List<AlbumRecord> albums = dataAccess.getArtistAlbums(artistId);
            TrackSelectionModel trackSelectionModel = new TrackSelectionModel();
            List<YearAndLoadedFxml> loadedFxmlsByYear = new ArrayList<>();
            for (AlbumRecord album : albums) {
                List<TrackRecord> albumTracks = dataAccess.getAlbumTracks(album.getId());
                Integer yearFromTracks = getYearFromTracks(albumTracks);
                log.debug("For album {} year {}", album.getName(), yearFromTracks);
                LoadedFxml<LibraryArtistAlbumController> loaded = fxmlLoader.load(
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
                );
                loadedFxmlsByYear.add(new YearAndLoadedFxml(yearFromTracks, loaded));
            }
            List<Parent> rootElements = loadedFxmlsByYear
                    .stream()
                    .sorted(comparing(YearAndLoadedFxml::year, nullsLast(naturalOrder())))
                    .map(YearAndLoadedFxml::loadedFxml)
                    .map(LoadedFxml::parent)
                    .toList();
            runOnFxThread(() -> artistAlbumsView.getChildren().addAll(rootElements));
        });
        t.start();
    }

    public void setUserPrefs(Stage primaryStage) {
        userPreferencesService.restoreWindowState(primaryStage);
        DelayedAction delayedSaveWindowSize = new DelayedAction(500, TimeUnit.MILLISECONDS);
        InvalidationListener invalidationListener = o ->
                delayedSaveWindowSize.reSchedule(() ->
                        userPreferencesService.saveWindowState(primaryStage));

        primaryStage.widthProperty().addListener(invalidationListener);
        primaryStage.heightProperty().addListener(invalidationListener);
        primaryStage.xProperty().addListener(invalidationListener);
        primaryStage.yProperty().addListener(invalidationListener);
        primaryStage.maximizedProperty().addListener(invalidationListener);

        volumeControl.setValue(userPreferencesService.getVolume());
        volumeControl.setOnMouseReleased(_ ->
                userPreferencesService.saveVolume((int) Math.round(volumeControl.getValue()))
        );

        librarySplitPane.setDividerPositions(userPreferencesService.getDividerPositions());
        DelayedAction delayedSaveDividerPosition = new DelayedAction(500, TimeUnit.MILLISECONDS);

        InvalidationListener splitPanePositionListener = o ->
                delayedSaveDividerPosition.reSchedule(() ->
                        userPreferencesService.saveSplitPositions(librarySplitPane.getDividerPositions()));

        librarySplitPane.getDividers().getFirst().positionProperty().addListener(splitPanePositionListener);
        librarySplitPane.getDividers().getLast().positionProperty().addListener(splitPanePositionListener);
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
        runOnFxThread(() -> {
            if (event.getMaxProgress() > 0) {
                progressCanvasController.setProgress(
                        Duration.seconds(event.getProgress()),
                        Duration.seconds(event.getMaxProgress())
                );
            } else {
                progressCanvasController.setProgressZero();
            }

            currentTrackName.setText(event.getProcessName());
            currentTrackAlbum.setText(event.getDescription());
            currentTrackName.setText("");
            timeSinceStart.setText(String.valueOf(event.getProgress()));
            timeRemaining.setText('-' + String.valueOf(event.getMaxProgress() - event.getProgress()));

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
        libraryTagEditTabController.showTags(trackInfo);
    }

    @SneakyThrows
    private void onLibraryDataUpdated() {
        artistsControllers.clear();
        artistList.getChildren().clear();
        log.info("Update UI from library in separate thread");
        Thread t = new Thread(() -> {
            for (ArtistRecord artist : dataAccess.getAllArtists()) {
                runOnFxThread(() -> {
                    LoadedFxml<LibraryArtistController> loadedFxml = fxmlLoader.load(
                            FxmlView.LIBRARY_ARTIST_ITEM,
                            new ArtistItemBundle(artist, artistSelectionModel)
                    );
                    Parent parent = loadedFxml.parent();
                    artistsControllers.add(loadedFxml.controller());
                    artistList.getChildren().add(parent);
                });
            }
        });
        t.start();
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

        TrackRecord currentTrack = playlistModel.getCurrentlyPlayingItem().get().getTrackRecord();
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
