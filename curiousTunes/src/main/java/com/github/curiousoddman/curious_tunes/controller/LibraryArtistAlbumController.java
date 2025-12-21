package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistAlbumController implements Initializable {
    private final DataAccess dataAccess;
    public ImageView albumImage;
    public Label albumTitle;
    public Label albumDetails;
    public GridPane albumTracksGrid;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        if (resources instanceof ArtistAlbumBundle albumBundle) {
            AlbumRecord albumRecord = albumBundle.getAlbumRecord();
            if (albumRecord.getImage() != null) {
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(albumRecord.getImage())) {
                    albumImage.setImage(new Image(byteArrayInputStream));
                }
            }

            albumTitle.setText(albumRecord.getName());
            albumDetails.setText("empty details..."); // FIXME
            List<TrackRecord> albumsTracks = dataAccess.getAlbumsTracks(List.of(albumRecord.getId()));
            int tracksPerColumn = albumsTracks.size() <= 10
                    ? albumsTracks.size()
                    : (albumsTracks.size() / 2);

            Iterator<TrackRecord> iterator = albumsTracks.iterator();
            int row = 0;
            int col = 0;
            while (iterator.hasNext()) {
                TrackRecord trackRecord = iterator.next();
                Label child = new Label(trackRecord.getTrackNumber() + ": " + trackRecord.getTitle() + " :: " + trackRecord.getDuration());
                albumTracksGrid.add(child, col, row);
                if (row + 1 == tracksPerColumn) {
                    col++;
                    row = 0;
                } else {
                    row++;
                }
            }
        }
    }

    public void onAlbumImageHover(MouseEvent mouseEvent) {

    }

    public void onAlbumImageUnhover(MouseEvent mouseEvent) {

    }
}
