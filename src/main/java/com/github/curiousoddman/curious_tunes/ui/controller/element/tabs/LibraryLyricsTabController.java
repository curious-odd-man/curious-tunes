package com.github.curiousoddman.curious_tunes.ui.controller.element.tabs;

import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.domain.lyrics.LyricsService;
import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Font;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryLyricsTabController implements Initializable {
    private final LyricsService lyricsService;
    private final DataAccess dataAccess;
    private final UserPreferencesService userPreferencesService;

    @FXML
    public ToggleButton editButton;
    @FXML
    public Button saveButton;
    @FXML
    public TextArea lyricsTextArea;
    @FXML
    public Button searchLyricsButton;
    @FXML
    public Slider fontSizeSlider;

    private PlaylistItem playlistItem;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        editButton.setDisable(true);
        double lyricsFontSize = userPreferencesService.getLyricsFontSize();
        fontSizeSlider.setValue(lyricsFontSize);
        lyricsTextArea.setFont(Font.font(lyricsFontSize));
        fontSizeSlider.setOnMouseReleased(event -> {
            double size = fontSizeSlider.getValue();
            if (size != lyricsTextArea.getFont().getSize()) {
                lyricsTextArea.setFont(Font.font(size));
                userPreferencesService.saveLyricsFontSize(size);
            }
        });
    }

    public void showLyrics(PlaylistItem playlistItem) {
        this.playlistItem = playlistItem;
        String lyrics = playlistItem.getLyrics();
        lyricsTextArea.setText(lyrics);
        editButton.setSelected(false);
        editButton.setDisable(false);
    }

    @FXML
    public void onEditButtonClick(ActionEvent actionEvent) {
        lyricsTextArea.setEditable(true);
        saveButton.setDisable(!editButton.isSelected());
    }

    @FXML
    public void onSaveButtonClick(ActionEvent actionEvent) {
        lyricsTextArea.setEditable(false);
        saveButton.setDisable(true);
        editButton.setSelected(false);

        Thread t = new Thread(() -> {
            log.info("Saving updated lyrics to {}", playlistItem.getFileLocation());
            dataAccess.storeTrackOverride(playlistItem.getTrackRecord(), TRACK.LYRICS, playlistItem.getLyrics());
            playlistItem.setLyrics(lyricsTextArea.getText());
            playlistItem.getTrackRecord().update(TRACK.LYRICS);
            log.info("Update completed...");
        }, "Update metadata");
        t.start();
    }

    @FXML
    public void onFindLyrics(ActionEvent actionEvent) {
        lyricsService.findLyricsAsync(
                playlistItem.getTrackArtist().getName(),
                playlistItem.getTrackAlbum().getName(),
                playlistItem.getTrackRecord().getTitle(),
                lyrics -> Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    editButton.setSelected(true);
                    lyricsTextArea.setText(lyrics);
                }),
                () -> log.warn("Unable to find lyrics...")
        );
    }
}
