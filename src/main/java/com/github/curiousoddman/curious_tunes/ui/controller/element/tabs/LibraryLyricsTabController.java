package com.github.curiousoddman.curious_tunes.ui.controller.element.tabs;

import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.domain.lyrics.LyricsService;
import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
import com.github.curiousoddman.curious_tunes.event.UserNotificationEvent;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;

/**
 * Lyrics of the track that is playing: a clean reading view, an editor, and an empty state.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryLyricsTabController implements Initializable {
    private static final double MIN_FONT_SIZE = 10;
    private static final double MAX_FONT_SIZE = 40;

    private final LyricsService             lyricsService;
    private final DataAccess                dataAccess;
    private final UserPreferencesService    userPreferencesService;
    private final ApplicationEventPublisher eventPublisher;

    @FXML
    public ToggleButton      editButton;
    @FXML
    public Button            saveButton;
    @FXML
    public Button            revertButton;
    @FXML
    public Button            searchLyricsButton;
    @FXML
    public ProgressIndicator searchProgress;
    @FXML
    public Label             unsavedLabel;
    @FXML
    public Label             sourceLabel;
    @FXML
    public Button            fontSmallerButton;
    @FXML
    public Button            fontLargerButton;
    @FXML
    public ScrollPane        readScroll;
    @FXML
    public Label             lyricsReadLabel;
    @FXML
    public TextArea          lyricsTextArea;
    @FXML
    public VBox              emptyBox;
    @FXML
    public Label             emptyTitle;
    @FXML
    public Label             emptyHint;
    @FXML
    public Button            emptyFindButton;

    private PlaylistItem playlistItem;
    private String       savedText = "";      // lyrics as stored; Save and Revert need a different text
    private double       fontSize;
    private boolean      searching;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        editButton.setDisable(true);
        lyricsTextArea.textProperty()
                      .addListener((obs, oldText, newText) -> {
                          lyricsReadLabel.setText(newText);
                          refreshState();
                      });
        applyFontSize(userPreferencesService.getLyricsFontSize());
        refreshState();
    }

    /**
     * Shows the lyrics of this track (the track that has just started playing).
     */
    public void showLyrics(PlaylistItem playlistItem) {
        this.playlistItem = playlistItem;
        String lyrics = normalizeNewlines(playlistItem.getLyrics());
        savedText = lyrics == null ? "" : lyrics;
        applyFontSize(userPreferencesService.getLyricsFontSize());       // may have been changed in the playback window
        editButton.setSelected(false);
        editButton.setDisable(false);
        sourceLabel.setText("");
        lyricsTextArea.setText(savedText);
        refreshState();
    }

    private boolean isEdited() {
        return playlistItem != null && !Objects.equals(lyricsTextArea.getText(), savedText);
    }

    /**
     * One place that decides what is visible: the reading view, the editor or the empty state.
     */
    private void refreshState() {
        boolean edited    = isEdited();
        boolean editing   = playlistItem != null && editButton.isSelected();
        boolean noText    = lyricsTextArea.getText() == null || lyricsTextArea.getText()
                                                                              .isBlank();
        boolean showEmpty = playlistItem == null || (!editing && noText);

        setShown(readScroll, !editing && !showEmpty);
        setShown(lyricsTextArea, editing);
        setShown(emptyBox, !editing && showEmpty);

        if (playlistItem == null) {
            emptyTitle.setText("Nothing is playing");
            emptyHint.setText("The lyrics of the track that is playing appear here.");
        } else {
            emptyTitle.setText("No lyrics for this track");
            emptyHint.setText("Look for them online, or write them yourself with Edit.");
        }
        setShown(emptyFindButton, playlistItem != null);

        editButton.setText(editing ? "Done" : "Edit");
        saveButton.setDisable(!edited);
        revertButton.setDisable(!edited);
        searchLyricsButton.setDisable(playlistItem == null || searching);
        emptyFindButton.setDisable(searching);
        setShown(unsavedLabel, edited);
        setShown(searchProgress, searching);
        setShown(sourceLabel, sourceLabel.getText() != null && !sourceLabel.getText()
                                                                           .isBlank());
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    @FXML
    public void onEditButtonClick(ActionEvent actionEvent) {
        refreshState();
    }

    @FXML
    public void onRevertButtonClick(ActionEvent actionEvent) {
        lyricsTextArea.setText(savedText);
        sourceLabel.setText("");
        refreshState();
    }

    @FXML
    public void onSaveButtonClick(ActionEvent actionEvent) {
        if (playlistItem == null) {
            return;
        }
        savedText = lyricsTextArea.getText();
        editButton.setSelected(false);
        sourceLabel.setText("");
        refreshState();

        PlaylistItem item = playlistItem;
        String       text = savedText;
        Thread t = new Thread(() -> {
            log.info("Saving updated lyrics to {}", item.getFileLocation());
            dataAccess.storeTrackOverride(item.getTrackRecord(), TRACK.LYRICS, item.getLyrics());
            item.setLyrics(text);
            item.getTrackRecord()
                .update(TRACK.LYRICS);
            log.info("Update completed...");
            eventPublisher.publishEvent(UserNotificationEvent.info(this, "Lyrics saved"));
        }, "Update metadata");
        t.start();
    }

    @FXML
    public void onFindLyrics(ActionEvent actionEvent) {
        if (playlistItem == null || searching) {
            return;
        }
        searching = true;
        sourceLabel.setText("");
        refreshState();
        lyricsService.findLyricsWithSourceAsync(
                playlistItem.getTrackArtist()
                            .getName(),
                playlistItem.getTrackAlbum()
                            .getName(),
                playlistItem.getTrackRecord()
                            .getTitle(),
                result -> Platform.runLater(() -> {
                    searching = false;
                    editButton.setSelected(true);
                    lyricsTextArea.setText(normalizeNewlines(result.lyrics()));     // Save and Revert become available
                    sourceLabel.setText("Found on " + result.source());
                    refreshState();
                    eventPublisher.publishEvent(UserNotificationEvent.info(this, "Lyrics found. Review them and press Save."));
                }),
                () -> Platform.runLater(() -> {
                    searching = false;
                    refreshState();
                    log.warn("Unable to find lyrics...");
                    eventPublisher.publishEvent(UserNotificationEvent.error(this, "No lyrics found online for this track"));
                })
        );
    }

    @FXML
    public void onFontSmaller(ActionEvent actionEvent) {
        changeFontSize(-1);
    }

    @FXML
    public void onFontLarger(ActionEvent actionEvent) {
        changeFontSize(1);
    }

    private void changeFontSize(double delta) {
        double size = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize + delta));
        applyFontSize(size);
        userPreferencesService.saveLyricsFontSize(size);
    }

    private void applyFontSize(double size) {
        fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
        lyricsTextArea.setFont(Font.font(fontSize));
        lyricsReadLabel.setFont(Font.font(fontSize));
        fontSmallerButton.setDisable(fontSize <= MIN_FONT_SIZE);
        fontLargerButton.setDisable(fontSize >= MAX_FONT_SIZE);
    }

    public static String normalizeNewlines(String s) {
        return s == null ? null : s.replaceAll("\\R", "\n");
    }
}
