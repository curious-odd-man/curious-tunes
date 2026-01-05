package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.ShowArtistAlbums;
import com.github.curiousoddman.curious_tunes.model.ArtistSelectionModel;
import com.github.curiousoddman.curious_tunes.model.PlaylistAddMode;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistItemBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.BORDERED_ITEM;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.SELECTED_ITEM;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistController implements Initializable {
    private final ApplicationEventPublisher eventPublisher;

    public ImageView artistImageView;
    public Label artistNameLabel;
    public HBox pane;

    private ArtistRecord artistRecord;
    private ArtistSelectionModel artistSelectionModel;


    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        pane.getStyleClass().add(BORDERED_ITEM);
        if (resources instanceof ArtistItemBundle bundle) {
            ArtistRecord artist = bundle.getArtist();
            //String icon = bundle.getIcon(); // FIXME:
            //artistImageView.setImage(new Image(Path.of(icon).toUri().toString()));
            artistNameLabel.setText(artist.getName());
            this.artistRecord = artist;
            this.artistSelectionModel = bundle.getArtistSelectionModel();
        }
    }

    @FXML
    public void onArtistClicked(MouseEvent mouseEvent) {
        artistSelectionModel.getOptionalSelectedItem().ifPresent(LibraryArtistController::clearSelection);
        artistSelectionModel.select(this);
        artistNameLabel.getParent().getStyleClass().add(SELECTED_ITEM);
        eventPublisher.publishEvent(new ShowArtistAlbums(this, artistRecord));
    }

    public void clearSelection() {
        artistNameLabel.getParent().getStyleClass().remove(SELECTED_ITEM);
    }

    @FXML
    public void onPlayClicked(ActionEvent actionEvent) {
        AddToPlaylistEvent event = AddToPlaylistEvent
                .builder()
                .source(this)
                .artistRecord(artistRecord)
                .shuffle(Shuffle.SKIP)
                .playlistAddMode(PlaylistAddMode.REPLACE)
                .build();
        eventPublisher.publishEvent(event);
    }

    @FXML
    public void onAddPlayClicked(ActionEvent actionEvent) {
        AddToPlaylistEvent event = AddToPlaylistEvent
                .builder()
                .source(this)
                .artistRecord(artistRecord)
                .shuffle(Shuffle.SKIP)
                .playlistAddMode(PlaylistAddMode.APPEND)
                .build();
        eventPublisher.publishEvent(event);
    }

    @FXML
    public void onShuffleClicked(ActionEvent actionEvent) {
        AddToPlaylistEvent event = AddToPlaylistEvent
                .builder()
                .source(this)
                .artistRecord(artistRecord)
                .shuffle(Shuffle.AFTER_ADDING_TO_PLAYLIST)
                .playlistAddMode(PlaylistAddMode.REPLACE)
                .build();
        eventPublisher.publishEvent(event);
    }

    @FXML
    public void onAddShuffledClicked(ActionEvent actionEvent) {
        AddToPlaylistEvent event = AddToPlaylistEvent
                .builder()
                .source(this)
                .artistRecord(artistRecord)
                .shuffle(Shuffle.BEFORE_ADDING_TO_PLAYLIST)
                .playlistAddMode(PlaylistAddMode.APPEND)
                .build();
        eventPublisher.publishEvent(event);
    }
}
