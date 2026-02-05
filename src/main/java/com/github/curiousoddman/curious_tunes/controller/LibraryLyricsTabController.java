package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryLyricsTabController implements Initializable {
    public ToggleButton editSaveButton;
    public TextArea lyricsTextArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        switchEditable();
    }

    @FXML
    public void onEditSaveButtonClick(ActionEvent actionEvent) {
        switchEditable();
    }

    private void switchEditable() {
        boolean editable = editSaveButton.isSelected();
        lyricsTextArea.setEditable(editable);
        editSaveButton.setText(editable ? "💾 Save" : "🖊 Enable Edit");
    }

    public void showLyrics(ReadOnlyObjectProperty<TrackRecord> observable) {
        TrackRecord trackRecord = observable.getValue();
        String lyrics = trackRecord.getLyrics();
        lyricsTextArea.setText(lyrics);

        observable.addListener((observable1, oldValue, newValue) -> {
            if (newValue != null) {
                lyricsTextArea.setText(newValue.getLyrics());
            }
        });
    }
}
