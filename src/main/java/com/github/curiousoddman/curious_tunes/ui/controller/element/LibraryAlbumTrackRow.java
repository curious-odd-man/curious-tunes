package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.event.EditTagsForTrackEvent;
import com.github.curiousoddman.curious_tunes.event.EditTagsForTracksEvent;
import com.github.curiousoddman.curious_tunes.event.FindLyricsForTrackEvent;
import com.github.curiousoddman.curious_tunes.event.UserNotificationEvent;
import com.github.curiousoddman.curious_tunes.model.LibraryDragPayload;
import com.github.curiousoddman.curious_tunes.model.NowPlayingMarker;
import com.github.curiousoddman.curious_tunes.model.TrackSelectionModel;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.util.FileManager;
import com.github.curiousoddman.curious_tunes.util.TrackActions;
import com.github.curiousoddman.curious_tunes.util.UiIcons;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.layout.Priority;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.github.curiousoddman.curious_tunes.util.TimeUtils.secondsToHumanTime;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.LIBRARY_TRACK_PLAYING;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.SELECTED_ITEM;

/**
 * One track of an album in the library: no-lyrics hint, number (a play icon on hover and while playing), title
 * and duration. Click to select (Shift/Ctrl for ranges and toggles), double-click or click the number to play,
 * right-click for the menu, drag into the queue.
 * <p>
 * Built in code rather than from FXML (doc/performance-review.md, item 2.4): an artist's album view has one row per
 * track, and loading an FXML file plus a Spring bean for each of them made opening an artist slow. The same layout
 * and style classes as the former {@code library-album-track.fxml}. The context menu is built on the first
 * right-click, not with every row.
 */
public class LibraryAlbumTrackRow extends HBox {
    private final TrackInfo                 trackInfo;
    private final TrackSelectionModel       trackSelectionModel;
    private final ApplicationEventPublisher eventPublisher;
    private final NowPlayingMarker          nowPlayingMarker;
    private final LibraryDragPayload        dragPayload;

    private final Label warningLabel     = new Label();
    private final Label trackNumberLabel = new Label();
    private final Label trackTitleLabel  = new Label();
    private final Label trackTimeLabel   = new Label();

    private ContextMenu contextMenu;
    private MenuItem    findLyricsItem;
    private MenuItem    editTagsItem;
    private MenuItem    showInFolderItem;
    private boolean     playing;
    private boolean     hovering;

    // Held by the row; the marker only keeps a weak reference to it (rows are rebuilt often).
    private final ChangeListener<Integer> nowPlayingListener = (obs, oldId, newId) -> updateNowPlayingStyle();

    public LibraryAlbumTrackRow(TrackInfo trackInfo,
                                TrackSelectionModel trackSelectionModel,
                                ApplicationEventPublisher eventPublisher,
                                NowPlayingMarker nowPlayingMarker,
                                LibraryDragPayload dragPayload) {
        this.trackInfo = trackInfo;
        this.trackSelectionModel = trackSelectionModel;
        this.eventPublisher = eventPublisher;
        this.nowPlayingMarker = nowPlayingMarker;
        this.dragPayload = dragPayload;
        buildLayout();
        trackSelectionModel.register(trackInfo, this);
        wireEvents();
        trackSelectionModel.getSelectedItems()
                           .addListener((ListChangeListener<TrackInfo>) change -> updateSelectionStyle());
        nowPlayingMarker.trackIdProperty()
                        .addListener(new WeakChangeListener<>(nowPlayingListener));
        updateNowPlayingStyle();
    }

    public TrackInfo getTrackInfo() {
        return trackInfo;
    }

    private void buildLayout() {
        getStyleClass().add("library-album-track");
        setFillHeight(false);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setSpacing(10);
        setPadding(new Insets(2, 10, 0, 10));

        warningLabel.getStyleClass()
                    .add("no-lyrics-hint");
        warningLabel.setGraphic(UiIcons.icon("mdi2m-music-note-off", 14));
        warningLabel.setVisible(false);
        if (trackInfo.getLyrics() == null || trackInfo.getLyrics()
                                                      .isBlank()) {
            warningLabel.setVisible(true);
            warningLabel.setTooltip(new Tooltip("No lyrics available for this track"));
        }

        trackNumberLabel.getStyleClass()
                        .add("text-body");
        trackNumberLabel.setAlignment(Pos.CENTER_RIGHT);
        trackNumberLabel.setMinWidth(28);
        trackNumberLabel.setPrefWidth(28);

        trackTitleLabel.getStyleClass()
                       .add("text-body");
        trackTitleLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        trackTitleLabel.setText(trackInfo.getTitle());

        trackTimeLabel.getStyleClass()
                      .add("text-body");
        trackTimeLabel.setAlignment(Pos.CENTER_RIGHT);
        trackTimeLabel.setMaxHeight(Double.MAX_VALUE);
        trackTimeLabel.setMinWidth(60);
        trackTimeLabel.setPrefWidth(60);
        trackTimeLabel.setText(secondsToHumanTime(trackInfo.getDuration()));

        HBox.setHgrow(warningLabel, Priority.NEVER);
        HBox.setHgrow(trackNumberLabel, Priority.NEVER);
        HBox.setHgrow(trackTitleLabel, Priority.ALWAYS);
        HBox.setHgrow(trackTimeLabel, Priority.NEVER);
        getChildren().addAll(warningLabel, trackNumberLabel, trackTitleLabel, trackTimeLabel);
    }

    private void wireEvents() {
        setOnMouseClicked(this::onMouseClicked);

        // The play icon takes the place of the number while the pointer is over the row.
        setOnMouseEntered(_ -> {
            hovering = true;
            updateNumberLabel();
        });
        setOnMouseExited(_ -> {
            hovering = false;
            updateNumberLabel();
        });
        trackNumberLabel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !event.isShiftDown() && !event.isShortcutDown()) {
                trackSelectionModel.selectOnly(trackInfo);
                TrackActions.playNow(eventPublisher, this, List.of(trackInfo));
                event.consume();
            }
        });

        // The selected tracks can be dragged into the queue.
        setOnDragDetected(event -> {
            if (!trackSelectionModel.isSelected(trackInfo)) {
                trackSelectionModel.selectOnly(trackInfo);
            }
            Dragboard        dragboard = startDragAndDrop(TransferMode.COPY);
            ClipboardContent content   = new ClipboardContent();
            content.putString(trackInfo.getTitle());
            dragboard.setContent(content);
            dragPayload.setTracks(selectedTracks());
            event.consume();
        });
        setOnDragDone(_ -> dragPayload.clear());
    }

    private void onMouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            // A right click inside the selection keeps it; outside it, the clicked track becomes the selection.
            if (!trackSelectionModel.isSelected(trackInfo)) {
                trackSelectionModel.selectOnly(trackInfo);
            }
            ensureContextMenu();
            boolean single = trackSelectionModel.getSelectedItems()
                                                .size() <= 1;
            findLyricsItem.setDisable(!single);
            showInFolderItem.setDisable(!single);
            editTagsItem.setText(single ? "Edit tags" : "Edit tags of " + trackSelectionModel.getSelectedItems()
                                                                                             .size() + " tracks");
            contextMenu.show(this, mouseEvent.getScreenX(), mouseEvent.getScreenY());
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
                TrackActions.playNow(eventPublisher, this, List.of(trackInfo));
            }
        }
    }

    private void ensureContextMenu() {
        if (contextMenu != null) {
            return;
        }
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
        playNow.setOnAction(_ -> TrackActions.playNow(eventPublisher, this, selectedTracks()));
        playNext.setOnAction(_ -> TrackActions.playNext(eventPublisher, this, selectedTracks()));
        addToQueue.setOnAction(_ -> TrackActions.addToQueue(eventPublisher, this, selectedTracks()));
        findLyricsItem.setOnAction(_ -> eventPublisher.publishEvent(new FindLyricsForTrackEvent(this, trackInfo)));
        editTagsItem.setOnAction(_ -> {
            List<TrackInfo> tracks = selectedTracks();
            eventPublisher.publishEvent(tracks.size() > 1
                    ? new EditTagsForTracksEvent(this, tracks)
                    : new EditTagsForTrackEvent(this, trackInfo));
        });
        showInFolderItem.setOnAction(_ -> {
            if (!FileManager.reveal(trackInfo.getFileLocation())) {
                eventPublisher.publishEvent(UserNotificationEvent.error(this, "Unable to open the file manager"));
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
        eventPublisher.publishEvent(UserNotificationEvent.info(this, "Copied to the clipboard"));
    }

    private List<TrackInfo> selectedTracks() {
        List<TrackInfo> selected = trackSelectionModel.getSelectedInOrder();
        return selected.isEmpty() ? List.of(trackInfo) : selected;
    }

    private void updateAccessibleText() {
        setAccessibleText(trackInfo.getTitle() + ", " + secondsToHumanTime(trackInfo.getDuration())
                + (playing ? ", playing now" : "")
                + (trackSelectionModel.isSelected(trackInfo) ? ", selected" : ""));
    }

    private void updateSelectionStyle() {
        boolean selected = trackSelectionModel.isSelected(trackInfo);
        updateAccessibleText();
        List<String> styleClasses = getStyleClass();
        if (selected && !styleClasses.contains(SELECTED_ITEM)) {
            styleClasses.add(SELECTED_ITEM);
        } else if (!selected) {
            styleClasses.remove(SELECTED_ITEM);
        }
    }

    private void updateNowPlayingStyle() {
        playing = trackInfo.getTrackId() != null
                && Objects.equals(nowPlayingMarker.trackIdProperty()
                                                  .get(), trackInfo.getTrackId());
        List<String> styleClasses = getStyleClass();
        styleClasses.remove(LIBRARY_TRACK_PLAYING);
        if (playing) {
            styleClasses.add(LIBRARY_TRACK_PLAYING);
        }
        updateNumberLabel();
        updateAccessibleText();
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
            trackNumberLabel.setText(trackInfo.getTrackNumber() == null ? "—" : String.valueOf(trackInfo.getTrackNumber()));
        }
    }
}
