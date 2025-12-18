package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.model.Album;
import com.github.curiousoddman.curious_tunes.model.Track;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Component
@Scope(SCOPE_PROTOTYPE)
public class LibraryArtistAlbumController implements Initializable {
    public ImageView albumImage;
    public Label albumTitle;
    public Label albumDetails;
    public GridPane albumTracksGrid;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources instanceof ArtistAlbumBundle albumBundle) {
            Album album = albumBundle.getAlbum();
            albumTitle.setText(album.getName());
            albumImage.setImage(new Image(Path.of(album.getImage()).toUri().toString()));
            albumDetails.setText(album.getTracks().size() + " tracks");
            List<Track> tracks = album.getTracks();
            for (int i = 0; i < tracks.size(); i++) {
                Track track = tracks.get(i);
                albumTracksGrid.addRow(i, new Label(track.getDuration() + " --> " + track.getName()));
            }
        }
    }

    public void onAlbumImageHover(MouseEvent mouseEvent) {

    }

    public void onAlbumImageUnhover(MouseEvent mouseEvent) {

    }
}
