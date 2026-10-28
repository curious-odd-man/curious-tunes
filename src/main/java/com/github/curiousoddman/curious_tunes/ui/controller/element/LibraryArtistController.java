package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistAddMode;
import com.github.curiousoddman.curious_tunes.util.Avatar;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.Normalizer;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistController implements Initializable {
    private final ApplicationEventPublisher eventPublisher;

    public StackPane avatar;
    @FXML
    public Circle    avatarCircle;
    @FXML
    public Label     avatarInitials;
    public Label     artistNameLabel;
    public HBox      pane;

    private ArtistRecord artistRecord;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // The row is created once per visible list cell and filled by setArtist(...) when the cell shows another artist.
    }

    public void setArtist(ArtistRecord artist) {
        this.artistRecord = artist;
        artistNameLabel.setText(artist.getName());
        avatarInitials.setText(Avatar.initials(artist.getName()));
        avatarCircle.setFill(Avatar.color(artist.getName()));
    }

    /**
     * Lower-cases and strips accents, so that "bjork" finds "Björk".
     */
    public static String fold(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                         .replaceAll("\\p{M}+", "")
                         .toLowerCase(Locale.ROOT);
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
