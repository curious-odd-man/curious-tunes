package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.event.EditTagsForTrackEvent;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.model.TrackSelectionModel;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumTrackBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.util.TimeUtils.secondsToHumanTime;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.SELECTED_ITEM;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistAlbumTrackController implements Initializable {
    @FXML
    public HBox rootHbox;
    @FXML
    public Label warningLabel;
    @FXML
    public Label trackNumberLabel;
    @FXML
    public Label trackTitleLabel;
    @FXML
    public Label trackTimeLabel;

    private final ApplicationEventPublisher applicationEventPublisher;
    private TrackSelectionModel trackSelectionModel;

    private TrackInfo trackInfo;
    private ContextMenu contextMenu;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        if (resources instanceof ArtistAlbumTrackBundle trackBundle) {
            trackInfo = trackBundle.getTrackInfo();
            trackNumberLabel.setText(String.valueOf(trackInfo.getTrackNumber()));
            trackTitleLabel.setText(trackInfo.getTitle());
            trackTimeLabel.setText(secondsToHumanTime(trackInfo.getDuration()));
            if (trackInfo.getLyrics() == null || trackInfo.getLyrics().isBlank()) {
                warningLabel.setVisible(true);
                warningLabel.setTooltip(new Tooltip("No lyrics available for this track"));
            }
            trackSelectionModel = trackBundle.getTrackSelectionModel();
        }

        contextMenu = new ContextMenu();
        MenuItem editTags = new MenuItem("Edit Tags");
        contextMenu.getItems().addAll(editTags);
        editTags.setOnAction(_ -> applicationEventPublisher.publishEvent(new EditTagsForTrackEvent(this, trackInfo)));
        trackSelectionModel
                .selectedItemProperty()
                .addListener(this::onTrackSelectionChanged);
    }

    private void onTrackSelectionChanged(Object observable, TrackInfo oldValue, TrackInfo newValue) {
        if (Objects.equals(oldValue, trackInfo)) {
            rootHbox.getStyleClass().remove(SELECTED_ITEM);
        } else if (Objects.equals(newValue, trackInfo)) {
            rootHbox.getStyleClass().add(SELECTED_ITEM);
        }
    }

    @FXML
    public void onMouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            contextMenu.show(rootHbox, mouseEvent.getScreenX(), mouseEvent.getScreenY());
        }
        trackSelectionModel.select(trackInfo);
    }
}
