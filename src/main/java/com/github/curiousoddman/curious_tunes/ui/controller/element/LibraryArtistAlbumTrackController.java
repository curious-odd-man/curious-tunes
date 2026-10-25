package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.event.EditTagsForTrackEvent;
import com.github.curiousoddman.curious_tunes.model.NowPlayingMarker;
import com.github.curiousoddman.curious_tunes.model.TrackSelectionModel;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumTrackBundle;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.util.TrackActions;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
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
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.util.TimeUtils.secondsToHumanTime;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.LIBRARY_TRACK_PLAYING;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.SELECTED_ITEM;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistAlbumTrackController implements Initializable {
    @FXML
    public HBox  rootHbox;
    @FXML
    public Label warningLabel;
    @FXML
    public Label trackNumberLabel;
    @FXML
    public Label trackTitleLabel;
    @FXML
    public Label trackTimeLabel;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final NowPlayingMarker          nowPlayingMarker;
    private       TrackSelectionModel       trackSelectionModel;

    private TrackInfo   trackInfo;
    private ContextMenu contextMenu;
    private MenuItem    editTagsItem;

    // Held by this controller; the marker only keeps a weak reference to it (rows are rebuilt often).
    private final ChangeListener<Integer> nowPlayingListener = (obs, oldId, newId) -> updateNowPlayingStyle();

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        if (resources instanceof ArtistAlbumTrackBundle trackBundle) {
            trackInfo = trackBundle.getTrackInfo();
            trackTitleLabel.setText(trackInfo.getTitle());
            trackTimeLabel.setText(secondsToHumanTime(trackInfo.getDuration()));
            if (trackInfo.getLyrics() == null || trackInfo.getLyrics()
                                                          .isBlank()) {
                warningLabel.setVisible(true);
                warningLabel.setTooltip(new Tooltip("No lyrics available for this track"));
            }
            trackSelectionModel = trackBundle.getTrackSelectionModel();
            trackSelectionModel.register(trackInfo);
        }

        contextMenu = new ContextMenu();
        MenuItem playNow    = new MenuItem("Play now");
        MenuItem playNext   = new MenuItem("Play next");
        MenuItem addToQueue = new MenuItem("Add to queue");
        editTagsItem = new MenuItem("Edit tags");
        contextMenu.getItems()
                   .addAll(playNow, playNext, addToQueue, new SeparatorMenuItem(), editTagsItem);
        playNow.setOnAction(_ -> TrackActions.playNow(applicationEventPublisher, this, selectedTracks()));
        playNext.setOnAction(_ -> TrackActions.playNext(applicationEventPublisher, this, selectedTracks()));
        addToQueue.setOnAction(_ -> TrackActions.addToQueue(applicationEventPublisher, this, selectedTracks()));
        editTagsItem.setOnAction(_ -> applicationEventPublisher.publishEvent(new EditTagsForTrackEvent(this, trackInfo)));

        trackSelectionModel
                .getSelectedItems()
                .addListener((ListChangeListener<TrackInfo>) change -> updateSelectionStyle());
        nowPlayingMarker.trackIdProperty()
                        .addListener(new WeakChangeListener<>(nowPlayingListener));
        updateNowPlayingStyle();
    }

    private List<TrackInfo> selectedTracks() {
        List<TrackInfo> selected = trackSelectionModel.getSelectedInOrder();
        return selected.isEmpty() ? List.of(trackInfo) : selected;
    }

    private void updateSelectionStyle() {
        boolean      selected     = trackSelectionModel.isSelected(trackInfo);
        List<String> styleClasses = rootHbox.getStyleClass();
        if (selected && !styleClasses.contains(SELECTED_ITEM)) {
            styleClasses.add(SELECTED_ITEM);
        } else if (!selected) {
            styleClasses.remove(SELECTED_ITEM);
        }
    }

    private void updateNowPlayingStyle() {
        boolean playing = trackInfo != null
                && trackInfo.getTrackId() != null
                && Objects.equals(nowPlayingMarker.trackIdProperty()
                                                  .get(), trackInfo.getTrackId());
        List<String> styleClasses = rootHbox.getStyleClass();
        styleClasses.remove(LIBRARY_TRACK_PLAYING);
        if (playing) {
            styleClasses.add(LIBRARY_TRACK_PLAYING);
        }
        trackNumberLabel.setText(playing ? "▶" : trackNumberText());
    }

    private String trackNumberText() {
        if (trackInfo == null) {
            return "";
        }
        return trackInfo.getTrackNumber() == null ? "—" : String.valueOf(trackInfo.getTrackNumber());
    }

    @FXML
    public void onMouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            // A right click inside the selection keeps it; outside it, the clicked track becomes the selection.
            if (!trackSelectionModel.isSelected(trackInfo)) {
                trackSelectionModel.selectOnly(trackInfo);
            }
            editTagsItem.setDisable(trackSelectionModel.getSelectedItems()
                                                       .size() > 1);
            contextMenu.show(rootHbox, mouseEvent.getScreenX(), mouseEvent.getScreenY());
            return;
        }
        if (mouseEvent.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (mouseEvent.isShortcutDown()) {
            trackSelectionModel.toggle(trackInfo);
        } else if (mouseEvent.isShiftDown()) {
            trackSelectionModel.selectRange(trackInfo);
        } else {
            trackSelectionModel.selectOnly(trackInfo);
            if (mouseEvent.getClickCount() > 1) {
                TrackActions.playNow(applicationEventPublisher, this, List.of(trackInfo));
            }
        }
    }
}
