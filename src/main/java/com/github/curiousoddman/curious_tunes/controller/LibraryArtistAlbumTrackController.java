package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumTrackBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.util.TimeUtils.secondsToHumanTime;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistAlbumTrackController implements Initializable {
    @FXML
    public Label warningLabel;
    @FXML
    public Label trackNumberLabel;
    @FXML
    public Label trackTitleLabel;
    @FXML
    public Label trackTimeLabel;

    private TrackRecord trackRecord;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        if (resources instanceof ArtistAlbumTrackBundle trackBundle) {
            trackRecord = trackBundle.getTrackRecord();
            trackNumberLabel.setText(String.valueOf(trackRecord.getTrackNumber()));
            trackTitleLabel.setText(trackRecord.getTitle());
            trackTimeLabel.setText(secondsToHumanTime(trackRecord.getDuration()));
            if (trackRecord.getLyrics() == null || trackRecord.getLyrics().isBlank()) {
                warningLabel.setVisible(true);
                warningLabel.setTooltip(new Tooltip("No lyrics available for this track"));
            }
        }
    }

    @FXML
    public void onMouseClicked(MouseEvent mouseEvent) {
        log.error("Not implemented yet");
    }
}
