package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.actions.services.PendingActionService;
import com.github.curiousoddman.curious_tunes.backend.lyrics.LyricsService;
import com.github.curiousoddman.curious_tunes.backend.tags.MetadataManager;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.PlaylistItem;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryLyricsTabController implements Initializable {
    private final MetadataManager metadataManager;
    private final PendingActionService pendingActionService;
    private final LyricsService lyricsService;

    @FXML
    public ToggleButton editButton;
    @FXML
    public Button saveButton;
    @FXML
    public TextArea lyricsTextArea;
    @FXML
    public Button searchLyricsButton;

    private ReadOnlyObjectProperty<PlaylistItem> trackRecordObservable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        editButton.setDisable(true);
    }

    public void showLyrics(ReadOnlyObjectProperty<PlaylistItem> observable) {
        PlaylistItem playlistItem = observable.getValue();
        TrackRecord trackRecord = playlistItem.getTrackRecord();
        String lyrics = trackRecord.getLyrics();
        lyricsTextArea.setText(lyrics);
        editButton.setSelected(false);
        if (trackRecordObservable == null) {
            trackRecordObservable = observable;
            trackRecordObservable.addListener((observable1, oldValue, newValue) -> {
                if (newValue != null) {
                    lyricsTextArea.setText(newValue.getTrackRecord().getLyrics());
                    editButton.setSelected(false);
                }
            });
        }
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

        TrackRecord trackRecord = trackRecordObservable.get().getTrackRecord();

        Thread t = new Thread(() -> {
            log.info("Saving updated lyrics to {}", trackRecord.getFileLocation());
            trackRecord.setLyrics(lyricsTextArea.getText());
            trackRecord.update(TRACK.LYRICS);
            pendingActionService.updateLyrics(lyricsTextArea.getText(), Path.of(trackRecord.getFileLocation()));
            log.info("Update completed...");
        }, "Update metadata");
        t.start();
    }

    @FXML
    public void onFindLyrics(ActionEvent actionEvent) {
        Thread t = new Thread(() -> {
            PlaylistItem playlistItem = trackRecordObservable.get();
            Optional<String> lyrics = lyricsService.findLyrics(
                    playlistItem.getTrackArtist().getName(),
                    playlistItem.getTrackAlbum().getName(),
                    playlistItem.getTrackRecord().getTitle()
            );

            if (lyrics.isEmpty()) {
                log.warn("Unable to find lyrics...");
                return;
            }

            Platform.runLater(() -> {
                saveButton.setDisable(false);
                editButton.setSelected(true);
                lyricsTextArea.setText(lyrics.get());
            });
        }, "Find lyrics online");
        t.start();
    }
}
