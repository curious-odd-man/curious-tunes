package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.bundle.PlaylistItemResourceBundle;
import com.github.curiousoddman.curious_tunes.util.ImageUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Lazy
@Slf4j
@Component
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pane.setStyle("-fx-border-width: 1px;  -fx-border-color: rgb(204,190,255);");
        contextMenu = new ContextMenu();
        MenuItem remove = new MenuItem("Remove");
        MenuItem moveToEnd = new MenuItem("Move to end");
        MenuItem moveToNext = new MenuItem("Move to next");
        contextMenu.getItems().addAll(remove, moveToEnd, moveToNext);
        // FIXME - implement actions

        if (resources instanceof PlaylistItemResourceBundle playlistItemResourceBundle) {
            AlbumRecord albumRecord = playlistItemResourceBundle.getAlbumRecord();
            ImageUtils.setImageIfPresent(albumRecord, image);
            TrackRecord trackRecord = playlistItemResourceBundle.getTrackRecord();
            rightText.setText(String.valueOf(trackRecord.getDuration()));
            topText.setText(trackRecord.getTitle());
            bottomText.setText(playlistItemResourceBundle.getArtist() + "  ---  " + albumRecord.getName());
        }
    }

    @FXML
    public void onPaneClicked(MouseEvent mouseEvent) {
        if (mouseEvent.isSecondaryButtonDown()) {
            contextMenu.show(pane, mouseEvent.getScreenX(), mouseEvent.getScreenY());
        }
    }
}
