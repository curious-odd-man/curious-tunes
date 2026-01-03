package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.model.PlaylistAddMode;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import com.github.curiousoddman.curious_tunes.util.ImageUtils;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import javafx.animation.FadeTransition;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.util.GlobalStyleClasses.BORDERED_ITEM;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistAlbumController implements Initializable {
    private final DataAccess dataAccess;
    private final ApplicationEventPublisher applicationEventPublisher;
    public ImageView albumImage;
    public Label albumTitle;
    public Label albumDetails;
    public VBox tracksLeftColumnVbox;
    public VBox tracksRightColumnVbox;
    public BorderPane pane;
    public ImageView playImageButton;

    private AlbumRecord albumRecord;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        pane.getStyleClass().add(BORDERED_ITEM);
        if (resources instanceof ArtistAlbumBundle albumBundle) {
            albumRecord = albumBundle.getAlbumRecord();
            ImageUtils.setImageIfPresent(albumRecord, albumImage);

            albumTitle.setText(albumRecord.getName());
            albumDetails.setText("empty details..."); // FIXME
            List<TrackRecord> albumsTracks = dataAccess.getAlbumTracks(albumRecord.getId());
            int tracksPerColumn = albumsTracks.size() <= 10
                    ? albumsTracks.size()
                    : (albumsTracks.size() / 2);

            Iterator<TrackRecord> iterator = albumsTracks.iterator();
            int row = 0;
            VBox col = tracksLeftColumnVbox;
            while (iterator.hasNext()) {
                TrackRecord trackRecord = iterator.next();
                Label child = new Label(trackRecord.getTrackNumber() + ": " + trackRecord.getTitle() + " :: " + TimeUtils.secondsToHumanTime(trackRecord.getDuration()));
                col.getChildren().add(child);
                if (row + 1 == tracksPerColumn) {
                    col = tracksRightColumnVbox;
                    row = 0;
                } else {
                    row++;
                }
            }
        }
    }

    public void onAlbumImageHover(MouseEvent mouseEvent) {
        fadePlayImageButtonTo(1);
    }

    public void onAlbumImageUnhover(MouseEvent mouseEvent) {
        fadePlayImageButtonTo(0);
    }

    public void onPlayImageClicked(MouseEvent mouseEvent) {
        AddToPlaylistEvent event = AddToPlaylistEvent
                .builder()
                .source(this)
                .albums(List.of(albumRecord))
                .shuffle(Shuffle.SKIP)
                .playlistAddMode(PlaylistAddMode.REPLACE)
                .build();
        applicationEventPublisher.publishEvent(event);
    }

    private void fadePlayImageButtonTo(int value) {
        FadeTransition ft = new FadeTransition(Duration.millis(250), playImageButton);
        ft.setFromValue(playImageButton.getOpacity());
        ft.setToValue(value);
        ft.play();
    }
}
