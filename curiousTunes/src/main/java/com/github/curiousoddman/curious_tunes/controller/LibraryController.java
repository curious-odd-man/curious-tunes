package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.AlbumsRepository;
import com.github.curiousoddman.curious_tunes.backend.ArtistRepository;
import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.config.StageManager;
import com.github.curiousoddman.curious_tunes.event.PlayNextEvent;
import com.github.curiousoddman.curious_tunes.event.PlayPauseEvent;
import com.github.curiousoddman.curious_tunes.event.PlayPreviousEvent;
import com.github.curiousoddman.curious_tunes.event.ShowArtistAlbums;
import com.github.curiousoddman.curious_tunes.model.Album;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistItemBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.RescanBundle;
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
import java.util.List;
import java.util.ResourceBundle;

@Lazy
@Component
@RequiredArgsConstructor
public class LibraryController implements Initializable {
    private final StageManager stageManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ArtistRepository artistRepository;
    private final AlbumsRepository albumsRepository;
    private final FxmlLoader fxmlLoader;


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

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        List<String> artists = artistRepository.getArtists();
        for (String artist : artists) {
            LoadedFxml<LibraryArtistController> loadedFxml = fxmlLoader.load(
                    FxmlView.LIBRARY_ARTIST_ITEM,
                    new ArtistItemBundle(artist, "C:\\Users\\curious\\Pictures\\Desktop Backgrounds\\Image 17.jpg")
            );
            Parent parent = loadedFxml.parent();
            artistList.getChildren().add(parent);
        }
    }

    @EventListener
    @SneakyThrows
    public void onShowArtistAlbumEvent(ShowArtistAlbums showArtistAlbums) {
        String artist = showArtistAlbums.getArtist();
        artistTitle.setText(artist);
        artistAlbumsView.getChildren().remove(1, artistAlbumsView.getChildren().size());
        List<Album> albums = albumsRepository.getAlbumsForArtist(artist);
        for (Album album : albums) {
            LoadedFxml<LibraryArtistAlbumController> loadedFxml = fxmlLoader.load(
                    FxmlView.LIBRARY_ARTIST_ALBUM,
                    new ArtistAlbumBundle(artist, album)
            );
            artistAlbumsView.getChildren().add(loadedFxml.parent());
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
