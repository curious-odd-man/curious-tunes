package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.actions.services.PendingActionService;
import com.github.curiousoddman.curious_tunes.backend.tags.MetadataManager;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
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
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryLyricsTabController implements Initializable {
    private final MetadataManager metadataManager;
    private final PendingActionService pendingActionService;

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
            trackRecord.setLyrics(lyricsTextArea.getText());
            trackRecord.update(TRACK.LYRICS);
            pendingActionService.updateLyrics(lyricsTextArea.getText(), Path.of(trackRecord.getFileLocation()));
            log.info("Update completed...");
        }, "Update metadata");
        t.start();
    }
}
