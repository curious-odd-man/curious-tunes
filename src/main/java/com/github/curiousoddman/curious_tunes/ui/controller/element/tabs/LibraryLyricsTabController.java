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
import java.util.Objects;
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
    private String       savedText = "";      // lyrics as stored; Save is enabled only when the text differs

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        editButton.setDisable(true);
        saveButton.setDisable(true);
        lyricsTextArea.textProperty().addListener((obs, oldText, newText) -> updateSaveButton());
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
        String lyrics = normalizeNewlines(playlistItem.getLyrics());
        savedText = lyrics == null ? "" : lyrics;
        lyricsTextArea.setEditable(false);
        lyricsTextArea.setText(lyrics);
        editButton.setSelected(false);
        editButton.setDisable(false);
        updateSaveButton();
    }

    private void updateSaveButton() {
        boolean edited = playlistItem != null && !Objects.equals(lyricsTextArea.getText(), savedText);
        saveButton.setDisable(!edited);
    }

    @FXML
    public void onEditButtonClick(ActionEvent actionEvent) {
        lyricsTextArea.setEditable(editButton.isSelected());
    }

    @FXML
    public void onSaveButtonClick(ActionEvent actionEvent) {
        if (playlistItem == null) {
            return;
        }
        savedText = lyricsTextArea.getText();
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
        if (playlistItem == null) {
            return;
        }
        lyricsService.findLyricsAsync(
                playlistItem.getTrackArtist().getName(),
                playlistItem.getTrackAlbum().getName(),
                playlistItem.getTrackRecord().getTitle(),
                lyrics -> Platform.runLater(() -> {
                    editButton.setSelected(true);
                    lyricsTextArea.setEditable(true);
                    lyricsTextArea.setText(normalizeNewlines(lyrics));     // enables Save if the text is different
                }),
                () -> log.warn("Unable to find lyrics...")
        );
    }

    public static String normalizeNewlines(String s) {
        return s == null ? null : s.replaceAll("\\R", "\n");
    }
}
