package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.ShowInLibraryEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.PlaybackState;
import com.github.curiousoddman.curious_tunes.model.bundle.PlaylistItemResourceBundle;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistDragHandler;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItemMovement;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final Map<PlaylistItem, PlaylistItemController> playlistItemControllers = new HashMap<>();

    /**
     * Rows that are shown now. Identity based: the same track may be in the queue twice.
     */
    private final Map<PlaylistItem, Row> rows = new IdentityHashMap<>();

    private record Row(Node node, PlaylistItemController controller) {
    }

    private final PlaylistModel             playlistModel;
    private final FxmlLoader                fxmlLoader;
    private final ApplicationEventPublisher eventPublisher;

    @FXML
    private VBox       playlistVbox;
    @FXML
    private ScrollPane playlistScroll;
    @FXML
    private Label      queueSummaryLabel;

    // Drag state
    private       int                 dragFromIndex       = -1;
    private final PlaylistDragHandler playlistDragHandler = new PlaylistDragHandler() {
        @Override
        public void dragCompleted() {
            dragFromIndex = -1;
        }

        @Override
        public void dragInitiatedOn(AnchorPane pane) {
            dragFromIndex = playlistVbox.getChildren()
                                        .indexOf(pane);
            Dragboard        db = pane.startDragAndDrop(TransferMode.MOVE);
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
                    db.setDragView(dragImage(pane, count));
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
        public void dragDropped(AnchorPane pane, boolean below) {
            int toIndex = playlistVbox.getChildren()
                                      .indexOf(pane) + (below ? 1 : 0);
            if (dragFromIndex >= 0 && dragFromIndex != toIndex) {
                playlistModel.moveItemTo(dragFromIndex, toIndex);
                updatePlaylist(); // rebuild to reflect new order
            }
        }

        @Override
        public void dragDroppedAtEnd() {
            if (dragFromIndex >= 0) {
                playlistModel.moveItemTo(dragFromIndex, playlistVbox.getChildren()
                                                                    .size());
                updatePlaylist();
            }
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateSummary();

        // Scroll the list while a track is dragged close to its top or bottom edge.
        playlistScroll.addEventFilter(DragEvent.DRAG_OVER, event -> {
            double zone = 36;
            double y    = event.getY();
            if (y < zone) {
                playlistScroll.setVvalue(Math.max(0, playlistScroll.getVvalue() - 0.02));
            } else if (y > playlistScroll.getHeight() - zone) {
                playlistScroll.setVvalue(Math.min(1, playlistScroll.getVvalue() + 0.02));
            }
        });

        // Free space under the last row: drop here to move the track to the end.
        // (Rows consume the drag events themselves, so only the free space reaches these handlers.)
        playlistScroll.setOnDragOver(event -> {
            if (dragFromIndex >= 0 && event.getDragboard()
                                           .hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        playlistScroll.setOnDragDropped(event -> {
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
        PlaylistItemController playlistItemController = playlistItemControllers.get(playlistItem);
        if (playlistItemController == null) {
            log.error("NPE: null for {}", playlistItem.getTitle());
        } else {
            playlistItemController.updateStyleFromPlayerStatus();
        }
        if (playerStatusEvent.getStatus() == PlaybackState.LAUNCHING) {
            Platform.runLater(() -> scrollToItem(playlistItem, true));
        }
    }

    @FXML
    public void onScrollToPlayingClick(ActionEvent actionEvent) {
        playlistModel.getCurrentlyPlayingItem()
                     .ifPresent(item -> scrollToItem(item, false));
    }

    /**
     * @param onlyIfHidden when true the list is scrolled only if the row is not fully visible
     */
    private void scrollToItem(PlaylistItem item, boolean onlyIfHidden) {
        int index = playlistModel.getPlaylistItems()
                                 .indexOf(item);
        ObservableList<Node> rows = playlistVbox.getChildren();
        if (index < 0 || index >= rows.size()) {
            return;
        }
        playlistScroll.applyCss();
        playlistScroll.layout();

        double viewport = playlistScroll.getViewportBounds()
                                        .getHeight();
        double extra = playlistVbox.getHeight() - viewport;
        if (extra <= 0) {
            return;     // everything fits
        }
        Node row = rows.get(index);
        double rowTop = row.getBoundsInParent()
                           .getMinY();
        double rowBottom = row.getBoundsInParent()
                              .getMaxY();
        double viewTop = playlistScroll.getVvalue() * extra;
        if (onlyIfHidden && rowTop >= viewTop && rowBottom <= viewTop + viewport) {
            return;
        }
        double target = rowTop - (viewport - (rowBottom - rowTop)) / 2;     // center the row
        playlistScroll.setVvalue(Math.max(0, Math.min(1, target / extra)));
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
     * Brings the rows in line with the queue. Rows of tracks that are still in the queue are kept (so their
     * selection and style stay, and the scroll position is preserved); only new tracks get new rows.
     */
    private void updatePlaylist() {
        runOnFxThread(this::syncRows);
    }

    private void syncRows() {
        List<PlaylistItem>     items  = playlistModel.getPlaylistItems();
        Map<PlaylistItem, Row> synced = new IdentityHashMap<>();
        List<Node>             nodes  = new ArrayList<>(items.size());
        boolean                reused = false;
        for (PlaylistItem item : items) {
            Row row = rows.get(item);
            if (row == null) {
                row = buildRow(item);
            } else {
                reused = true;
            }
            synced.put(item, row);
            nodes.add(row.node());
        }

        rows.clear();
        rows.putAll(synced);
        playlistItemControllers.clear();
        synced.forEach((item, row) -> playlistItemControllers.put(item, row.controller()));

        ObservableList<Node> children = playlistVbox.getChildren();
        if (!children.equals(nodes)) {
            double top = scrollTop();
            children.setAll(nodes);
            if (reused) {
                restoreScrollTop(top);
            } else {
                playlistScroll.setVvalue(0);        // a completely new queue starts at the top
            }
        }
        updateSummary();
    }

    private double scrollTop() {
        double extra = Math.max(0, playlistVbox.getHeight() - playlistScroll.getViewportBounds()
                                                                            .getHeight());
        return playlistScroll.getVvalue() * extra;
    }

    private void restoreScrollTop(double top) {
        playlistScroll.applyCss();
        playlistScroll.layout();
        double extra = playlistVbox.getHeight() - playlistScroll.getViewportBounds()
                                                                .getHeight();
        playlistScroll.setVvalue(extra <= 0 ? 0 : Math.max(0, Math.min(1, top / extra)));
    }

    private Row buildRow(PlaylistItem playlistItem) {
        LoadedFxml<PlaylistItemController> loadedFxml = fxmlLoader.load(
                FxmlView.PLAYLIST_ITEM,
                new PlaylistItemResourceBundle(
                        playlistItem.getArtistName(),
                        playlistItem,
                        playlistModel,
                        playlistDragHandler
                )
        );
        PlaylistItemController controller = loadedFxml.controller();
        controller.updateStyleFromPlayerStatus();
        return new Row(loadedFxml.parent(), controller);
    }

    public void onUpButtonClick(ActionEvent actionEvent) {
        moveSelectedUp();
    }

    public void moveSelectedUp() {
        List<PlaylistItemMovement> movements = playlistModel.moveSelectedUp();
        ObservableList<Node>       nodes     = playlistVbox.getChildren();
        movements.forEach(m -> swapNodes(nodes, m.fromIndex(), m.toIndex()));
    }

    public void onDownButtonClick(ActionEvent actionEvent) {
        moveSelectedDown();
    }

    public void moveSelectedDown() {
        List<PlaylistItemMovement> movements = playlistModel.moveSelectedDown();
        ObservableList<Node>       nodes     = playlistVbox.getChildren();
        movements.forEach(m -> swapNodes(nodes, m.fromIndex(), m.toIndex()));
    }

    public void selectAll() {
        playlistModel.getSelectionModel()
                     .selectAll();
    }

    private void swapNodes(ObservableList<Node> children, int i1, int i2) {
        if (i1 < 0 || i2 < 0 || i1 >= children.size() || i2 >= children.size()) {
            return;
        }
        Region placeholder = new Region();
        Node   tmp         = children.set(i1, placeholder);
        Node   node2       = children.set(i2, tmp);
        children.set(i1, node2);
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
        alert.initOwner(playlistVbox.getScene()
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
