package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.HashMap;
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

    private final PlaylistModel playlistModel;
    private final FxmlLoader    fxmlLoader;

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
        }

        @Override
        public void removeItem(PlaylistItem playlistItem) {
            playlistModel.selectOnly(playlistItem);
            removeSelected();
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

    private void updatePlaylist() {
        List<PlaylistItem> playlistItems = playlistModel.getPlaylistItems();
        playlistItemControllers.clear();
        runOnFxThread(() -> {
            ObservableList<Node> playlistNodes = playlistVbox.getChildren();
            playlistNodes.clear();

            for (PlaylistItem playlistItem : playlistItems) {
                playlistNodes.add(buildRow(playlistItem));
            }
            updateSummary();
        });
    }

    private Node buildRow(PlaylistItem playlistItem) {

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
        playlistItemControllers.put(playlistItem, controller);
        Parent parent = loadedFxml.parent();
        controller.updateStyleFromPlayerStatus();
        return parent;
    }

    @FXML
    public void onUpButtonClick(ActionEvent actionEvent) {
        List<PlaylistItemMovement> movements = playlistModel.moveSelectedUp();
        ObservableList<Node>       nodes     = playlistVbox.getChildren();
        movements.forEach(m -> swapNodes(nodes, m.fromIndex(), m.toIndex()));
    }

    @FXML
    public void onDownButtonClick(ActionEvent actionEvent) {
        List<PlaylistItemMovement> movements = playlistModel.moveSelectedDown();
        ObservableList<Node>       nodes     = playlistVbox.getChildren();
        movements.forEach(m -> swapNodes(nodes, m.fromIndex(), m.toIndex()));
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

    /**
     * Removes the selected track from the queue. Used by the toolbar button, the Delete key and the context menu.
     */
    public void removeSelected() {
        PlaylistItem item = playlistModel.getSelected();
        if (item == null) {
            return;
        }

        int idx = playlistModel.getPlaylistItems()
                               .indexOf(item);
        playlistModel.deleteSelected();

        if (idx >= 0 && idx < playlistVbox.getChildren()
                                          .size()) {
            playlistVbox.getChildren()
                        .remove(idx);
        }
        playlistItemControllers.remove(item);
        updateSummary();
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
        playlistItemControllers.clear();
        playlistVbox.getChildren()
                    .clear();
        updateSummary();
    }
}