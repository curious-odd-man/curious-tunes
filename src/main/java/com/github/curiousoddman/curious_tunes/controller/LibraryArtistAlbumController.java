package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistAddMode;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumTrackBundle;
import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import com.github.curiousoddman.curious_tunes.util.ImageUtils;
import javafx.animation.FadeTransition;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
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

import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.BORDERED_ITEM;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistAlbumController implements Initializable {
    private final DataAccess dataAccess;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final FxmlLoader fxmlLoader;
    public ImageView albumImage;
    public Label albumTitle;
    public Label albumDetails;
    public VBox tracksLeftColumnVbox;
    public VBox tracksRightColumnVbox;
    public BorderPane pane;
    public ImageView playImageButton;

    private AlbumInfo albumInfo;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        pane.getStyleClass().add(BORDERED_ITEM);
        if (resources instanceof ArtistAlbumBundle albumBundle) {
            albumInfo = albumBundle.getAlbumInfo();
            ImageUtils.setImageIfPresent(albumInfo, albumImage);

            albumTitle.setText(albumInfo.getName());
            albumDetails.setText("empty details..."); // FIXME
            List<TrackRecord> albumsTracks = dataAccess.getAlbumTracks(albumInfo.getId());
            int tracksPerColumn = albumsTracks.size() <= 10
                    ? albumsTracks.size()
                    : (albumsTracks.size() / 2);

            Iterator<TrackRecord> iterator = albumsTracks.iterator();
            int row = 0;
            VBox col = tracksLeftColumnVbox;
            while (iterator.hasNext()) {
                TrackRecord trackRecord = iterator.next();
                LoadedFxml<LibraryArtistAlbumTrackController> loadedFxml = fxmlLoader.load(
                        FxmlView.LIBRARY_ALBUM_TRACK,
                        new ArtistAlbumTrackBundle(albumInfo.toTrackInfo(trackRecord), albumBundle.getTrackSelectionModel())
                );
                Parent parent = loadedFxml.parent();
                col.getChildren().add(parent);
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
                .albums(List.of(albumInfo.getAlbumRecord()))
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
