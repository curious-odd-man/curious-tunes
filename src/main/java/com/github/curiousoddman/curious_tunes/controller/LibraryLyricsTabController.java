package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.tags.MetadataManager;
import com.github.curiousoddman.curious_tunes.backend.tags.MetadataTags;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryLyricsTabController implements Initializable {
    private final MetadataManager metadataManager;

    public ToggleButton editButton;
    public Button saveButton;
    public TextArea lyricsTextArea;

    private ReadOnlyObjectProperty<TrackRecord> trackRecordObservable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        editButton.setDisable(true);
    }

    public void showLyrics(ReadOnlyObjectProperty<TrackRecord> observable) {
        TrackRecord trackRecord = observable.getValue();
        String lyrics = trackRecord.getLyrics();
        lyricsTextArea.setText(lyrics);
        editButton.setSelected(false);
        if (trackRecordObservable == null) {
            trackRecordObservable = observable;
            trackRecordObservable.addListener((observable1, oldValue, newValue) -> {
                if (newValue != null) {
                    lyricsTextArea.setText(newValue.getLyrics());
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

        TrackRecord trackRecord = trackRecordObservable.get();

        Thread t = new Thread(() -> {
            log.info("Saving updated lyrics to {}", trackRecord.getFileLocation());

            MetadataTags metadata = metadataManager.getMetadata(Path.of(trackRecord.getFileLocation()));
            String metadataLyrics = metadata.getLyrics();
            if (metadataLyrics.equals(trackRecord.getLyrics())) {
                trackRecord.setLyrics(lyricsTextArea.getText());
                metadata.setLyrics(lyricsTextArea.getText());
                try {
                    metadata.updateFile();
                    trackRecord.update(TRACK.LYRICS);
                } catch (IOException e) {
                    log.error("Failed to save file: ", e);
                }
            } else {
                log.error("Unable to save file changes - file contents are different from what was previously shown. Updating....");
                trackRecord.setLyrics(metadataLyrics);
                trackRecord.update(TRACK.LYRICS);
                Platform.runLater(() -> lyricsTextArea.setText(metadataLyrics));
            }
            log.info("Update completed...");
        }, "Update metadata");
        t.start();
    }
}
