package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.event.EditTagsForTrackEvent;
import com.github.curiousoddman.curious_tunes.event.FindLyricsForTrackEvent;
import com.github.curiousoddman.curious_tunes.event.UserNotificationEvent;
import com.github.curiousoddman.curious_tunes.model.LibraryDragPayload;
import com.github.curiousoddman.curious_tunes.model.NowPlayingMarker;
import com.github.curiousoddman.curious_tunes.model.TrackSelectionModel;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumTrackBundle;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.util.FileManager;
import com.github.curiousoddman.curious_tunes.util.TrackActions;
import com.github.curiousoddman.curious_tunes.util.UiIcons;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
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
import java.util.stream.Collectors;

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
    private final LibraryDragPayload        dragPayload;
    private       TrackSelectionModel       trackSelectionModel;

    private TrackInfo   trackInfo;
    private ContextMenu contextMenu;
    private MenuItem    findLyricsItem;
    private MenuItem    editTagsItem;
    private MenuItem    showInFolderItem;
    private boolean     playing;
    private boolean     hovering;

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
            trackSelectionModel.register(trackInfo, rootHbox);
        }

        buildContextMenu();

        // The play icon takes the place of the number while the pointer is over the row.
        rootHbox.setOnMouseEntered(_ -> {
            hovering = true;
            updateNumberLabel();
        });
        rootHbox.setOnMouseExited(_ -> {
            hovering = false;
            updateNumberLabel();
        });
        trackNumberLabel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !event.isShiftDown() && !event.isShortcutDown()) {
                trackSelectionModel.selectOnly(trackInfo);
                TrackActions.playNow(applicationEventPublisher, this, List.of(trackInfo));
                event.consume();
            }
        });

        // The selected tracks can be dragged into the queue.
        rootHbox.setOnDragDetected(event -> {
            if (!trackSelectionModel.isSelected(trackInfo)) {
                trackSelectionModel.selectOnly(trackInfo);
            }
            Dragboard        dragboard = rootHbox.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content   = new ClipboardContent();
            content.putString(trackInfo.getTitle());
            dragboard.setContent(content);
            dragPayload.setTracks(selectedTracks());
            event.consume();
        });
        rootHbox.setOnDragDone(_ -> dragPayload.clear());

        trackSelectionModel
                .getSelectedItems()
                .addListener((ListChangeListener<TrackInfo>) change -> updateSelectionStyle());
        nowPlayingMarker.trackIdProperty()
                        .addListener(new WeakChangeListener<>(nowPlayingListener));
        updateNowPlayingStyle();
    }

    private void buildContextMenu() {
        contextMenu = new ContextMenu();
        MenuItem playNow    = new MenuItem("Play now");
        MenuItem playNext   = new MenuItem("Play next");
        MenuItem addToQueue = new MenuItem("Add to queue");
        findLyricsItem = new MenuItem("Find lyrics");
        editTagsItem = new MenuItem("Edit tags");
        showInFolderItem = new MenuItem("Show in file manager");
        MenuItem copyInfo = new MenuItem("Copy title and artist");
        contextMenu.getItems()
                   .addAll(playNow, playNext, addToQueue,
                           new SeparatorMenuItem(),
                           findLyricsItem, editTagsItem,
                           new SeparatorMenuItem(),
                           showInFolderItem, copyInfo);
        playNow.setOnAction(_ -> TrackActions.playNow(applicationEventPublisher, this, selectedTracks()));
        playNext.setOnAction(_ -> TrackActions.playNext(applicationEventPublisher, this, selectedTracks()));
        addToQueue.setOnAction(_ -> TrackActions.addToQueue(applicationEventPublisher, this, selectedTracks()));
        findLyricsItem.setOnAction(_ -> applicationEventPublisher.publishEvent(new FindLyricsForTrackEvent(this, trackInfo)));
        editTagsItem.setOnAction(_ -> applicationEventPublisher.publishEvent(new EditTagsForTrackEvent(this, trackInfo)));
        showInFolderItem.setOnAction(_ -> {
            if (!FileManager.reveal(trackInfo.getFileLocation())) {
                applicationEventPublisher.publishEvent(
                        UserNotificationEvent.error(this, "Unable to open the file manager"));
            }
        });
        copyInfo.setOnAction(_ -> copyTitleAndArtist());
    }

    private void copyTitleAndArtist() {
        String text = selectedTracks().stream()
                                      .map(track -> track.getTrackArtist()
                                                         .getName() + " — " + track.getTitle())
                                      .collect(Collectors.joining(System.lineSeparator()));
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard()
                 .setContent(content);
        applicationEventPublisher.publishEvent(UserNotificationEvent.info(this, "Copied to the clipboard"));
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
        playing = trackInfo != null
                && trackInfo.getTrackId() != null
                && Objects.equals(nowPlayingMarker.trackIdProperty()
                                                  .get(), trackInfo.getTrackId());
        List<String> styleClasses = rootHbox.getStyleClass();
        styleClasses.remove(LIBRARY_TRACK_PLAYING);
        if (playing) {
            styleClasses.add(LIBRARY_TRACK_PLAYING);
        }
        updateNumberLabel();
    }

    /**
     * The track number; a play icon instead while the pointer is over the row, and while the track plays.
     */
    private void updateNumberLabel() {
        if (playing || hovering) {
            trackNumberLabel.setText("");
            trackNumberLabel.setGraphic(UiIcons.icon(UiIcons.PLAY, 14));
        } else {
            trackNumberLabel.setGraphic(null);
            trackNumberLabel.setText(trackNumberText());
        }
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
            boolean single = trackSelectionModel.getSelectedItems()
                                                .size() <= 1;
            findLyricsItem.setDisable(!single);
            editTagsItem.setDisable(!single);
            showInFolderItem.setDisable(!single);
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
