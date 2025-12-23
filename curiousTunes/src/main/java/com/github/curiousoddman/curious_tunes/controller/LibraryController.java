package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.backend.player.CurrentPlaylistService;
import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.config.StageManager;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.*;
import com.github.curiousoddman.curious_tunes.model.ArtistSelectionModel;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistItemBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.RescanBundle;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.backend.tags.FilesScanningService.LIBRARY_SCAN;
import static javafx.application.Platform.runLater;

@Lazy
@Component
@RequiredArgsConstructor
public class LibraryController implements Initializable {
    private final StageManager stageManager;
    private final ApplicationEventPublisher eventPublisher;
    private final FxmlLoader fxmlLoader;
    private final DataAccess dataAccess;

    @FXML
    public Button buttonPrevious;
    @FXML
    public Button buttonPlayPause;
    @FXML
    public Button buttonNext;
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
    public ToggleButton shuffleButton;
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
    public VBox playlistVbox;

    private final List<LibraryArtistController> artistsControllers = new ArrayList<>();
    private final CurrentPlaylistService currentPlaylistService;
    public ArtistSelectionModel artistSelectionModel;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        artistSelectionModel = new ArtistSelectionModel(artistsControllers);
        onLibraryDataUpdated();
    }

    @EventListener
    public void onPlaylistUpdatedEvent(PlaylistUpdatedEvent playlistUpdatedEvent) {
        Platform.runLater(() -> {
            List<TrackRecord> tracks = currentPlaylistService.getTracks();
            ObservableList<Node> playlist = playlistVbox.getChildren();
            playlist.clear();
            for (TrackRecord track : tracks) {
                playlist.add(new Label(track.getTrackNumber() + ". " + track.getTitle() + " --> " + track.getDuration()));
            }
        });
    }

    @EventListener
    @SneakyThrows
    public void onShowArtistAlbumEvent(ShowArtistAlbums showArtistAlbums) {
        int artistId = showArtistAlbums.getArtistRecord().getId();
        String artistName = showArtistAlbums.getArtistRecord().getName();
        artistTitle.setText(artistName);
        artistAlbumsView.getChildren().remove(1, artistAlbumsView.getChildren().size());
        List<AlbumRecord> albums = dataAccess.getArtistAlbums(artistId);
        for (AlbumRecord album : albums) {
            LoadedFxml<LibraryArtistAlbumController> loadedFxml = fxmlLoader.load(
                    FxmlView.LIBRARY_ARTIST_ALBUM,
                    new ArtistAlbumBundle(artistName, album)
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
    public void onPreviousClick(ActionEvent actionEvent) {
        eventPublisher.publishEvent(new PlayPreviousEvent(this));
    }

    @FXML
    public void onPlayPauseClick(ActionEvent actionEvent) {
        eventPublisher.publishEvent(new PlayPauseEvent(this));
    }

    @FXML
    public void onNextClick(ActionEvent actionEvent) {
        eventPublisher.publishEvent(new PlayNextEvent(this, shuffleButton.isSelected()));
    }

    @FXML
    public void onShuffleButtonClick(ActionEvent actionEvent) {
        eventPublisher.publishEvent(new PlayNextEvent(this, shuffleButton.isSelected()));
    }

    @FXML
    @SneakyThrows
    public void onRescanMenuClicked(ActionEvent actionEvent) {
        Stage stage = new Stage();
        Parent root = fxmlLoader.load(FxmlView.RESCAN_MODAL, new RescanBundle("D:\\iTunes\\iTunes 1\\iTunes Media\\Music")).parent();
        stage.setScene(new Scene(root));
        stage.setTitle("Rescan library");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(buttonPrevious.getScene().getWindow());
        stage.showAndWait();
    }
}
