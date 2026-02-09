package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistModel;
import com.github.curiousoddman.curious_tunes.model.bundle.PlaylistItemResourceBundle;
import com.github.curiousoddman.curious_tunes.util.ImageUtils;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.*;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PlaylistItemController implements Initializable {
    @FXML
    public ImageView image;
    @FXML
    public Label rightText;
    @FXML
    public Label topText;
    @FXML
    public Label bottomText;
    @FXML
    public AnchorPane pane;
    private ContextMenu contextMenu;
    private PlaylistItem playlistItem;
    private PlaylistModel playlistSelectionModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pane.getStyleClass().add(BORDERED_ITEM);
        contextMenu = new ContextMenu();
        MenuItem remove = new MenuItem("Remove");
        MenuItem moveToEnd = new MenuItem("Move to end");
        MenuItem moveToNext = new MenuItem("Move to next");
        contextMenu.getItems().addAll(remove, moveToEnd, moveToNext);
        // FIXME - implement actions

        if (resources instanceof PlaylistItemResourceBundle playlistItemResourceBundle) {
            AlbumRecord albumRecord = playlistItemResourceBundle.getPlaylistItem().getTrackAlbum();
            ImageUtils.setImageIfPresent(albumRecord, image);
            playlistItem = playlistItemResourceBundle.getPlaylistItem();
            TrackRecord trackRecord = playlistItem.getTrackRecord();
            rightText.setText(TimeUtils.secondsToHumanTime(trackRecord.getDuration()));
            topText.setText(trackRecord.getTitle());
            bottomText.setText(playlistItemResourceBundle.getArtist() + "  ---  " + albumRecord.getName());
            playlistSelectionModel = playlistItemResourceBundle.getPlaylistModel();
            playlistSelectionModel
                    .getSelectedProperty()
                    .addListener(this::onPlaylistSelectionChanged);
        }
    }

    private void onPlaylistSelectionChanged(Object observable, PlaylistItem oldValue, PlaylistItem newValue) {
        if (Objects.equals(oldValue, playlistItem)) {
            pane.getStyleClass().remove(SELECTED_ITEM);
        } else if (Objects.equals(newValue, playlistItem)) {
            pane.getStyleClass().add(SELECTED_ITEM);
        }
    }

    @FXML
    public void onPaneClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            contextMenu.show(pane, mouseEvent.getScreenX(), mouseEvent.getScreenY());
        }
        playlistSelectionModel.select(playlistItem);
    }

    public void updateStyle() {
        String styleClass = switch (playlistItem.getPlaylistItemStatus()) {
            case PLAYING -> PLAYLIST_ITEM_PLAYING;
            case SKIPPED, QUEUED, FILE_NOT_FOUND, TO_BE_SKIPPED -> null;
            case PLAYED -> PLAYLIST_ITEM_PLAYED;
        };
        if (styleClass != null) {
            ObservableList<String> styleClassList = pane.getStyleClass();
            styleClassList.removeAll(Set.of(PLAYLIST_ITEM_PLAYING, PLAYLIST_ITEM_PLAYED));     // FIXME
            styleClassList.add(styleClass);
        }
    }
}
