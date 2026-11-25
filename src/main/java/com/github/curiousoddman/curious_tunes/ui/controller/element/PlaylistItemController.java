package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.PlaySpecificTrackEvent;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistDragHandler;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItemStatus;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistModel;
import com.github.curiousoddman.curious_tunes.util.CoverImages;
import com.github.curiousoddman.curious_tunes.util.ImageUtils;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import com.github.curiousoddman.curious_tunes.util.UiIcons;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.IntSupplier;

import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.BORDERED_ITEM;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.PLAYLIST_DRAG_GUIDE;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.PLAYLIST_DRAG_GUIDE_BELOW;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.PLAYLIST_ITEM_MISSING;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.PLAYLIST_ITEM_PLAYED;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.PLAYLIST_ITEM_PLAYING;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.PLAYLIST_ITEM_SKIPPED;
import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.SELECTED_ITEM;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PlaylistItemController implements Initializable {
    private static final double COVER_MIN_ROW_WIDTH = 230;

    private final ApplicationEventPublisher eventPublisher;
    @FXML
    public        ImageView                 image;
    @FXML
    public        Label                     rightText;
    @FXML
    public        Label                     topText;
    @FXML
    public        Label                     bottomText;
    @FXML
    public        AnchorPane                pane;
    private       ContextMenu               contextMenu;
    private       PlaylistItem              playlistItem;
    private       PlaylistModel             playlistSelectionModel;
    private       PlaylistDragHandler       playlistDragHandler;
    private       IntSupplier               rowIndex;
    private final Tooltip                   missingFileTooltip = new Tooltip();
    private final ListChangeListener<PlaylistItem> selectionListener = this::onPlaylistSelectionChanged;
    private       boolean                   missingFileTooltipInstalled;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pane.getStyleClass()
            .add(BORDERED_ITEM);
        contextMenu = new ContextMenu();
        MenuItem playNext      = new MenuItem("Play next");
        MenuItem moveToEnd     = new MenuItem("Move to end");
        MenuItem showInLibrary = new MenuItem("Show in library");
        MenuItem remove        = new MenuItem("Remove from queue");
        playNext.setOnAction(_ -> playlistDragHandler.moveToNext(playlistItem));
        moveToEnd.setOnAction(_ -> playlistDragHandler.moveToEnd(playlistItem));
        showInLibrary.setOnAction(_ -> playlistDragHandler.showInLibrary(playlistItem));
        remove.setOnAction(_ -> playlistDragHandler.removeItem(playlistItem));
        contextMenu.getItems()
                   .addAll(playNext, moveToEnd, showInLibrary, new SeparatorMenuItem(), remove);
        // The track that is playing stays where it is.
        contextMenu.setOnShowing(_ -> {
            boolean playing = playlistItem.getPlaylistItemStatus() == PlaylistItemStatus.PLAYING;
            playNext.setDisable(playing);
            moveToEnd.setDisable(playing);
        });

        // In a narrow queue pane the cover is dropped to leave room for the text.
        pane.widthProperty()
            .addListener((obs, oldWidth, width) -> {
                boolean showCover = width.doubleValue() >= COVER_MIN_ROW_WIDTH;
                image.setVisible(showCover);
                image.setManaged(showCover);
            });
        rightText.setContentDisplay(ContentDisplay.LEFT);
    }

    /**
     * Connects the row to the queue, once: the row is a cell of a virtualised list (doc/performance-review.md,
     * item 3.1), created for the visible part of the queue and then reused for other tracks through {@link #bind}.
     *
     * @param rowIndex the row's current position in the queue
     */
    public void setup(PlaylistModel playlistModel, PlaylistDragHandler dragHandler, IntSupplier rowIndex) {
        this.playlistSelectionModel = playlistModel;
        this.playlistDragHandler = dragHandler;
        this.rowIndex = rowIndex;
        playlistModel.getSelectionModel()
                     .getSelectedItems()
                     .addListener(selectionListener);
    }

    /**
     * Shows {@code item} in this row, replacing whatever the row showed before.
     */
    public void bind(PlaylistItem item) {
        playlistItem = item;
        AlbumRecord albumRecord = item.getTrackAlbum();
        TrackRecord trackRecord = item.getTrackRecord();
        image.setImage(CoverImages.placeholder());      // a reused row must not keep the previous track's cover
        ImageUtils.setImageIfPresent(albumRecord, image);
        rightText.setText(TimeUtils.secondsToHumanTime(trackRecord.getDuration()));
        topText.setText(trackRecord.getTitle());
        bottomText.setText(item.getArtistName() + " · " + albumRecord.getName());
        removeDropGuides();
        ObservableList<String> styleClasses = pane.getStyleClass();
        styleClasses.remove(SELECTED_ITEM);
        if (playlistSelectionModel.isSelected(item)) {
            styleClasses.add(SELECTED_ITEM);
        }
        updateStyleFromPlayerStatus();
    }

    /**
     * The row shows no track any more (its cell became empty).
     */
    public void unbind() {
        playlistItem = null;
        image.setImage(null);
    }

    public PlaylistItem getPlaylistItem() {
        return playlistItem;
    }

    /**
     * The row is no longer shown (its track left the queue): stop listening to the shared selection list. Without
     * this, the selection list keeps every row ever created alive, with its nodes and cover
     * (doc/performance-review.md, item 3.2), and notifies all of them on every selection change.
     */
    public void dispose() {
        if (playlistSelectionModel != null) {
            playlistSelectionModel.getSelectionModel()
                                  .getSelectedItems()
                                  .removeListener(selectionListener);
        }
        image.setImage(null);
    }

    private void onPlaylistSelectionChanged(ListChangeListener.Change<? extends PlaylistItem> change) {
        if (playlistItem == null) {
            return;                                     // an empty row
        }
        while (change.next()) {
            if (change.getRemoved()
                      .contains(playlistItem)) {   // Replaced = added + removed
                pane.getStyleClass()
                    .remove(SELECTED_ITEM);
            }

            if (change.getAddedSubList()
                      .contains(playlistItem)) {
                pane.getStyleClass()
                    .add(SELECTED_ITEM);
            }
        }
    }

    @FXML
    public void onPaneClicked(MouseEvent mouseEvent) {
        if (playlistItem == null) {
            return;
        }
        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            contextMenu.show(pane, mouseEvent.getScreenX(), mouseEvent.getScreenY());
        }

        if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() > 1) {
            eventPublisher.publishEvent(new PlaySpecificTrackEvent(this, playlistItem, false));
        }

        if (mouseEvent.isControlDown()) {
            playlistSelectionModel.toggleSelect(playlistItem);
        } else if (mouseEvent.isShiftDown()) {
            playlistSelectionModel.rangeSelect(playlistItem);
        } else {
            playlistSelectionModel.selectOnly(playlistItem);
        }
    }

    public void updateStyleFromPlayerStatus() {
        if (playlistItem == null) {
            return;
        }
        PlaylistItemStatus     status         = playlistItem.getPlaylistItemStatus();
        ObservableList<String> styleClassList = pane.getStyleClass();
        styleClassList.removeAll(PLAYLIST_ITEM_PLAYING, PLAYLIST_ITEM_PLAYED, PLAYLIST_ITEM_MISSING, PLAYLIST_ITEM_SKIPPED);
        switch (status) {
            case PLAYING -> styleClassList.add(PLAYLIST_ITEM_PLAYING);
            case PLAYED -> styleClassList.add(PLAYLIST_ITEM_PLAYED);
            case FILE_NOT_FOUND -> styleClassList.add(PLAYLIST_ITEM_MISSING);
            case SKIPPED, TO_BE_SKIPPED -> styleClassList.add(PLAYLIST_ITEM_SKIPPED);
            case QUEUED -> {
            }
        }

        // The icon next to the duration: play for the track that is playing, a check for one that was played,
        // a warning for a missing file. The state is not shown by a colour alone.
        switch (status) {
            case PLAYING -> rightText.setGraphic(UiIcons.icon(UiIcons.PLAY, 14));
            case PLAYED -> rightText.setGraphic(UiIcons.icon("mdi2c-check", 14));
            case FILE_NOT_FOUND -> rightText.setGraphic(UiIcons.icon("mdi2a-alert-circle-outline", 14));
            default -> rightText.setGraphic(null);
        }
        pane.setAccessibleText(playlistItem.getTitle() + ", " + playlistItem.getArtistName() + switch (status) {
            case PLAYING -> ", playing now";
            case PLAYED -> ", played";
            case FILE_NOT_FOUND -> ", file not found";
            case SKIPPED, TO_BE_SKIPPED -> ", skipped";
            case QUEUED -> "";
        });

        boolean missing = status == PlaylistItemStatus.FILE_NOT_FOUND;
        if (missing) {
            missingFileTooltip.setText("File not found:\n" + playlistItem.getFileLocation());
            if (!missingFileTooltipInstalled) {
                Tooltip.install(pane, missingFileTooltip);
                missingFileTooltipInstalled = true;
            }
        } else if (missingFileTooltipInstalled) {
            Tooltip.uninstall(pane, missingFileTooltip);
            missingFileTooltipInstalled = false;
        }
    }

    @FXML
    public void onDragDetected(MouseEvent mouseEvent) {
        playlistDragHandler.dragInitiatedOn(pane, rowIndex.getAsInt());
        if (!playlistSelectionModel.isSelected(playlistItem)) {
            playlistSelectionModel.selectOnly(playlistItem);
        }
        mouseEvent.consume();
    }

    @FXML
    public void onDragDone(DragEvent dragEvent) {
        removeDropGuides();
        playlistDragHandler.dragCompleted();
        dragEvent.consume();
    }

    @FXML
    public void onDragDropped(DragEvent dragEvent) {
        playlistDragHandler.dragDropped(rowIndex.getAsInt(), isLowerHalf(dragEvent));
        dragEvent.setDropCompleted(true);
        dragEvent.consume();
    }

    @FXML
    public void onDragOver(DragEvent dragEvent) {
        if (dragEvent.getGestureSource() != pane && dragEvent.getDragboard()
                                                             .hasString()) {
            dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            updateDropGuide(dragEvent);
        }
        dragEvent.consume();
    }

    @FXML
    public void onDragEntered(DragEvent dragEvent) {
        if (dragEvent.getGestureSource() != pane && dragEvent.getDragboard()
                                                             .hasString()) {
            updateDropGuide(dragEvent);
        }
        dragEvent.consume();
    }

    @FXML
    public void onDragExited(DragEvent dragEvent) {
        removeDropGuides();
        dragEvent.consume();
    }

    /**
     * The pointer in the lower half of the row means "drop after this row", in the upper half "drop before it".
     */
    private boolean isLowerHalf(DragEvent dragEvent) {
        return dragEvent.getY() > pane.getHeight() / 2;
    }

    private void updateDropGuide(DragEvent dragEvent) {
        boolean                below        = isLowerHalf(dragEvent);
        String                 wanted       = below ? PLAYLIST_DRAG_GUIDE_BELOW : PLAYLIST_DRAG_GUIDE;
        String                 other        = below ? PLAYLIST_DRAG_GUIDE : PLAYLIST_DRAG_GUIDE_BELOW;
        ObservableList<String> styleClasses = pane.getStyleClass();
        styleClasses.remove(other);
        if (!styleClasses.contains(wanted)) {
            styleClasses.add(wanted);
        }
    }

    private void removeDropGuides() {
        pane.getStyleClass()
            .removeAll(PLAYLIST_DRAG_GUIDE, PLAYLIST_DRAG_GUIDE_BELOW);
    }
}
