package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.bundle.PlaylistItemResourceBundle;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistDragHandler;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItemMovement;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistModel;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.ClipboardContent;
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
import java.util.ResourceBundle;

import static com.sun.javafx.util.Utils.runOnFxThread;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryPlaylistController implements Initializable {
    private final Map<PlaylistItem, PlaylistItemController> playlistItemControllers = new HashMap<>();

    private final PlaylistModel playlistModel;
    private final FxmlLoader fxmlLoader;

    @FXML
    private VBox playlistVbox;

    // Drag state
    private int dragFromIndex = -1;
    private final PlaylistDragHandler playlistDragHandler = new PlaylistDragHandler() {
        @Override
        public void dragCompleted() {
            dragFromIndex = -1;
        }

        @Override
        public void dragInitiatedOn(AnchorPane pane) {
            dragFromIndex = playlistVbox.getChildren().indexOf(pane);
            Dragboard db = pane.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(String.valueOf(dragFromIndex)); // payload: from-index
            db.setContent(cc);
        }

        @Override
        public void dragDropped(AnchorPane pane) {
            int toIndex = playlistVbox.getChildren().indexOf(pane);
            if (dragFromIndex >= 0 && dragFromIndex != toIndex) {
                playlistModel.moveItemTo(dragFromIndex, toIndex);
                updatePlaylist(); // rebuild to reflect new order
            }
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @EventListener
    public void onAddToPlaylist(AddToPlaylistEvent addToPlaylistEvent) {
        playlistModel.addItems(addToPlaylistEvent);
        updatePlaylist();
    }

    @EventListener
    public void onPlayerStatusEvent(PlayerStatusEvent playerStatusEvent) {
        playlistModel.updateStatus(playerStatusEvent);
        PlaylistItem playlistItem = playerStatusEvent.getPlaylistItem();
        PlaylistItemController playlistItemController = playlistItemControllers.get(playlistItem);
        if (playlistItemController == null) {
            log.error("NPE: null for {}", playlistItem.getTitle());
        } else {
            playlistItemController.updateStyle();
        }
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
        controller.updateStyle();
        return parent;
    }

    @FXML
    public void onUpButtonClick(ActionEvent actionEvent) {
        List<PlaylistItemMovement> movements = playlistModel.moveSelectedUp();
        ObservableList<Node> nodes = playlistVbox.getChildren();
        movements.forEach(m -> swapNodes(nodes, m.fromIndex(), m.toIndex()));
    }

    @FXML
    public void onDownButtonClick(ActionEvent actionEvent) {
        List<PlaylistItemMovement> movements = playlistModel.moveSelectedDown();
        ObservableList<Node> nodes = playlistVbox.getChildren();
        movements.forEach(m -> swapNodes(nodes, m.fromIndex(), m.toIndex()));
    }

    private void swapNodes(ObservableList<Node> children, int i1, int i2) {
        if (i1 < 0 || i2 < 0 || i1 >= children.size() || i2 >= children.size()) {
            return;
        }
        Region placeholder = new Region();
        Node tmp = children.set(i1, placeholder);
        Node node2 = children.set(i2, tmp);
        children.set(i1, node2);
    }

    @FXML
    public void onDeleteClick(ActionEvent actionEvent) {
        PlaylistItem item = playlistModel.getSelected();
        if (item == null) {
            return;
        }

        int idx = playlistModel.getPlaylistItems().indexOf(item);
        playlistModel.deleteSelected();

        if (idx >= 0 && idx < playlistVbox.getChildren().size()) {
            playlistVbox.getChildren().remove(idx);
        }
        playlistItemControllers.remove(item);
    }

    @FXML
    public void onShuffleClicked(ActionEvent actionEvent) {
        playlistModel.shuffle();
        updatePlaylist();
    }

    @FXML
    public void onClearPlaylistClicked(ActionEvent actionEvent) {
        playlistModel.clear();
        playlistItemControllers.clear();
        playlistVbox.getChildren().clear();
    }
}