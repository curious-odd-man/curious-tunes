package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.ShowInLibraryEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlaylistItemUpdatedEvent;
import com.github.curiousoddman.curious_tunes.model.LibraryDragPayload;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.PlaybackState;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistDragHandler;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistModel;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import com.github.curiousoddman.curious_tunes.util.UiStyles;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.sun.javafx.util.Utils.runOnFxThread;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryPlaylistController implements Initializable {
    /**
     * The rows showing a track now, by track. The queue is a virtualised list (doc/performance-review.md, item 3.1):
     * there are rows only for the visible part, reused as it scrolls. Identity based: the same track may be in the
     * queue twice.
     */
    private final Map<PlaylistItem, PlaylistItemController> boundRows = new IdentityHashMap<>();

    private final PlaylistModel             playlistModel;
    private final FxmlLoader                fxmlLoader;
    private final ApplicationEventPublisher eventPublisher;
    private final LibraryDragPayload        libraryDragPayload;

    @FXML
    private ListView<PlaylistItem> playlistList;
    @FXML
    private Label                  queueSummaryLabel;

    // Drag state
    private       int                 dragFromIndex       = -1;
    private final PlaylistDragHandler playlistDragHandler = new PlaylistDragHandler() {
        @Override
        public void dragCompleted() {
            dragFromIndex = -1;
        }

        @Override
        public void dragInitiatedOn(Node row, int index) {
            dragFromIndex = index;
            Dragboard        db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(String.valueOf(dragFromIndex)); // payload: from-index
            db.setContent(cc);

            // Several selected rows move together: show how many.
            List<PlaylistItem> items = playlistModel.getPlaylistItems();
            if (dragFromIndex >= 0 && dragFromIndex < items.size()
                    && playlistModel.isSelected(items.get(dragFromIndex))) {
                int count = playlistModel.getSelectionModel()
                                         .getSelectedIndices()
                                         .size();
                if (count > 1) {
                    db.setDragView(dragImage(row, count));
                }
            }
        }

        @Override
        public void removeItem(PlaylistItem playlistItem) {
            playlistModel.selectOnly(playlistItem);
            removeSelected();
        }

        @Override
        public void moveToNext(PlaylistItem playlistItem) {
            if (playlistModel.moveToNext(playlistItem)) {
                updatePlaylist();
            }
        }

        @Override
        public void moveToEnd(PlaylistItem playlistItem) {
            if (playlistModel.moveToEnd(playlistItem)) {
                updatePlaylist();
            }
        }

        @Override
        public void showInLibrary(PlaylistItem playlistItem) {
            eventPublisher.publishEvent(new ShowInLibraryEvent(LibraryPlaylistController.this, playlistItem));
        }

        @Override
        public void dragDropped(int index, boolean below) {
            int toIndex = index + (below ? 1 : 0);
            if (libraryDragPayload.isActive()) {
                insertFromLibrary(toIndex);
                return;
            }
            if (dragFromIndex >= 0 && dragFromIndex != toIndex) {
                playlistModel.moveItemTo(dragFromIndex, toIndex);
                updatePlaylist(); // rebuild to reflect new order
            }
        }

        @Override
        public void dragDroppedAtEnd() {
            int end = playlistModel.getPlaylistItems()
                                   .size();
            if (libraryDragPayload.isActive()) {
                insertFromLibrary(end);
                return;
            }
            if (dragFromIndex >= 0) {
                playlistModel.moveItemTo(dragFromIndex, end);
                updatePlaylist();
            }
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateSummary();

        // The app's own selection model (PlaylistModel) selects rows; the list must not have a selection of its own.
        playlistList.setSelectionModel(new NoSelectionModel<>());
        playlistList.setCellFactory(list -> new PlaylistCell());
        playlistList.setPlaceholder(new Label(""));

        // Scroll the list while a track is dragged close to its top or bottom edge.
        playlistList.addEventFilter(DragEvent.DRAG_OVER, event -> {
            double         zone = 36;
            double         y    = event.getY();
            VirtualFlow<?> flow = flow();
            if (flow == null) {
                return;
            }
            if (y < zone) {
                flow.scrollPixels(-8);
            } else if (y > playlistList.getHeight() - zone) {
                flow.scrollPixels(8);
            }
        });

        // Free space under the last row: drop here to move the track to the end.
        // (Rows consume the drag events themselves, so only the free space reaches these handlers.)
        playlistList.setOnDragOver(event -> {
            boolean fromLibrary = libraryDragPayload.isActive();
            if ((dragFromIndex >= 0 || fromLibrary) && event.getDragboard()
                                                            .hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        playlistList.setOnDragDropped(event -> {
            playlistDragHandler.dragDroppedAtEnd();
            event.setDropCompleted(true);
            event.consume();
        });
    }

    @EventListener
    public void onAddToPlaylist(AddToPlaylistEvent addToPlaylistEvent) {
        playlistModel.addItems(addToPlaylistEvent);
        updatePlaylist();
    }

    @EventListener
    public void onPlayerStatusEvent(PlayerStatusEvent playerStatusEvent) {
        playlistModel.updateStatus(playerStatusEvent);
        PlaylistItem           playlistItem           = playerStatusEvent.getPlaylistItem();
        PlaylistItemController playlistItemController = boundRows.get(playlistItem);
        if (playlistItemController != null) {          // else not scrolled into view: styled when it is
            playlistItemController.updateStyleFromPlayerStatus();
        }
        if (playerStatusEvent.getStatus() == PlaybackState.LAUNCHING) {
            Platform.runLater(() -> scrollToItem(playlistItem, true));
        }
    }

    /**
     * The state of an item changed (for example its file is missing): style its row again.
     */
    @EventListener
    public void onPlaylistItemUpdated(PlaylistItemUpdatedEvent event) {
        runOnFxThread(() -> {
            PlaylistItemController controller = boundRows.get(event.getPlaylistItem());
            if (controller != null) {
                controller.updateStyleFromPlayerStatus();
            }
        });
    }

    @FXML
    public void onScrollToPlayingClick(ActionEvent actionEvent) {
        playlistModel.getCurrentlyPlayingItem()
                     .ifPresent(item -> scrollToItem(item, false));
    }

    /**
     * @param onlyIfHidden when true the list is scrolled only if the row is not fully visible
     */
    /**
     * Tracks or albums dragged from the library are inserted where they were dropped.
     */
    private void insertFromLibrary(int toIndex) {
        playlistModel.insertItems(toIndex, libraryDragPayload.getTracks(), libraryDragPayload.getAlbums());
        libraryDragPayload.clear();
        updatePlaylist();
    }

    /**
     * @param onlyIfHidden when true the list is scrolled only if the row is not fully visible
     */
    private void scrollToItem(PlaylistItem item, boolean onlyIfHidden) {
        int index = indexOf(item);
        if (index < 0) {
            return;
        }
        VirtualFlow<?> flow = flow();
        if (flow == null || flow.getFirstVisibleCell() == null || flow.getLastVisibleCell() == null) {
            playlistList.scrollTo(index);
            return;
        }
        int first = flow.getFirstVisibleCell()
                        .getIndex();
        int last = flow.getLastVisibleCell()
                       .getIndex();
        if (onlyIfHidden && index > first && index < last) {
            return;                                     // fully visible (the first and last may be cut off)
        }
        int visible = Math.max(1, last - first);
        playlistList.scrollTo(Math.max(0, index - visible / 2));     // center the row
    }

    /**
     * Position of exactly this item (the same track may be in the queue twice).
     */
    private int indexOf(PlaylistItem item) {
        List<PlaylistItem> items = playlistModel.getPlaylistItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The list's scrolling part, or null before the list is shown.
     */
    private VirtualFlow<?> flow() {
        return playlistList.lookup(".virtual-flow") instanceof VirtualFlow<?> flow ? flow : null;
    }

    private void updateSummary() {
        List<PlaylistItem> items = playlistModel.getPlaylistItems();
        if (items.isEmpty()) {
            queueSummaryLabel.setText("Queue is empty");
            queueSummaryLabel.setTooltip(new Tooltip("Double-click a track, or use the play buttons on an artist or an album, to fill the queue"));
            return;
        }
        long totalSeconds = items.stream()
                                 .map(PlaylistItem::getDuration)
                                 .filter(Objects::nonNull)
                                 .mapToLong(Long::longValue)
                                 .sum();
        queueSummaryLabel.setText(items.size() + (items.size() == 1 ? " track · " : " tracks · ")
                + TimeUtils.secondsToHumanTime((int) totalSeconds));
        queueSummaryLabel.setTooltip(null);
    }

    /**
     * Brings the list in line with the queue, keeping the scroll position when the queue only changed (a completely
     * new queue starts at the top).
     */
    private void updatePlaylist() {
        runOnFxThread(this::syncRows);
    }

    private void syncRows() {
        List<PlaylistItem>           items = playlistModel.getPlaylistItems();
        ObservableList<PlaylistItem> shown = playlistList.getItems();
        if (!sameItems(shown, items)) {
            boolean reused = containsAny(shown, items);
            int     top    = firstVisibleIndex();
            shown.setAll(items);
            if (reused) {
                playlistList.scrollTo(Math.clamp(items.size() - 1, 0, top));
            } else {
                playlistList.scrollTo(0);
            }
        }
        boundRows.values()
                 .forEach(PlaylistItemController::updateStyleFromPlayerStatus);
        updateSummary();
    }

    private int firstVisibleIndex() {
        VirtualFlow<?> flow = flow();
        return flow == null || flow.getFirstVisibleCell() == null ? 0 : flow.getFirstVisibleCell()
                                                                            .getIndex();
    }

    private static boolean sameItems(List<PlaylistItem> a, List<PlaylistItem> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAny(List<PlaylistItem> before, List<PlaylistItem> after) {
        Map<PlaylistItem, Boolean> seen = new IdentityHashMap<>();
        before.forEach(item -> seen.put(item, true));
        for (PlaylistItem item : after) {
            if (seen.containsKey(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A row of the queue: one {@code playlist-item.fxml} per cell, loaded once and bound to whichever track the cell
     * shows. Only the visible rows exist (doc/performance-review.md, item 3.1).
     */
    private final class PlaylistCell extends ListCell<PlaylistItem> {
        private final Node                   node;
        private final PlaylistItemController controller;

        PlaylistCell() {
            LoadedFxml<PlaylistItemController> loaded = fxmlLoader.load(FxmlView.PLAYLIST_ITEM, null);
            node = loaded.parent();
            controller = loaded.controller();
            controller.setup(playlistModel, playlistDragHandler, this::getIndex);
            if (node instanceof Region region) {
                region.setMinWidth(0);
                region.prefWidthProperty()
                      .bind(playlistList.widthProperty()
                                        .subtract(20));     // room for the scroll bar: no horizontal scrolling
            }
            getStyleClass().add("playlist-cell");
            setText(null);
        }

        @Override
        protected void updateItem(PlaylistItem item, boolean empty) {
            PlaylistItem previous = controller.getPlaylistItem();
            if (previous != null && boundRows.get(previous) == controller) {
                boundRows.remove(previous);
            }
            super.updateItem(item, empty);
            if (empty || item == null) {
                controller.unbind();
                setGraphic(null);
                return;
            }
            controller.bind(item);
            boundRows.put(item, controller);
            setGraphic(node);
        }
    }

    public void onUpButtonClick(ActionEvent actionEvent) {
        moveSelectedUp();
    }

    public void moveSelectedUp() {
        if (!playlistModel.moveSelectedUp()
                          .isEmpty()) {
            syncRows();
        }
    }

    public void onDownButtonClick(ActionEvent actionEvent) {
        moveSelectedDown();
    }

    public void moveSelectedDown() {
        if (!playlistModel.moveSelectedDown()
                          .isEmpty()) {
            syncRows();
        }
    }

    public void selectAll() {
        playlistModel.getSelectionModel()
                     .selectAll();
    }

    @FXML
    public void onDeleteClick(ActionEvent actionEvent) {
        removeSelected();
    }

    public void removeSelected() {
        if (playlistModel.getSelectionModel()
                         .isEmpty()) {
            return;
        }
        playlistModel.deleteAllSelected();
        updatePlaylist();
    }

    @FXML
    public void onShuffleClicked(ActionEvent actionEvent) {
        playlistModel.shuffle();
        updatePlaylist();
    }

    @FXML
    public void onClearPlaylistClicked(ActionEvent actionEvent) {
        int count = playlistModel.getPlaylistItems()
                                 .size();
        if (count == 0) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove all " + count + (count == 1 ? " track" : " tracks") + " from the queue?",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Clear the queue?");
        alert.initOwner(playlistList.getScene()
                                    .getWindow());
        UiStyles.apply(alert);
        if (alert.showAndWait()
                 .filter(button -> button == ButtonType.OK)
                 .isEmpty()) {
            return;
        }
        playlistModel.clear();
        updatePlaylist();
    }

    /**
     * The row as a drag image with the number of dragged rows in a badge.
     */
    private static Image dragImage(Node row, int count) {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        ImageView rowImage = new ImageView(row.snapshot(parameters, null));
        rowImage.setOpacity(0.85);
        Label badge = new Label(String.valueOf(count));
        badge.getStyleClass()
             .add("drag-badge");
        StackPane stack = new StackPane(rowImage, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        UiStyles.apply(new Scene(stack));       // the badge style comes from the shared stylesheet
        return stack.snapshot(parameters, null);
    }
}
