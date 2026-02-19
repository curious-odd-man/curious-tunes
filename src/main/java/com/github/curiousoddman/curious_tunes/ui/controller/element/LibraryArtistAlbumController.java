package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumDiscBundle;
import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistAddMode;
import com.github.curiousoddman.curious_tunes.util.ImageUtils;
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
import java.util.*;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistAlbumController implements Initializable {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final FxmlLoader fxmlLoader;
    public ImageView albumImage;
    public Label albumTitle;
    public Label albumDetails;
    public VBox albumDiscsVbox;
    public BorderPane pane;
    public ImageView playImageButton;

    private final List<LibraryArtistAlbumDiscController> discControllers = new ArrayList<>();
    private AlbumInfo albumInfo;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        if (resources instanceof ArtistAlbumBundle albumBundle) {
            albumInfo = albumBundle.getAlbumInfo();
            ImageUtils.setImageIfPresent(albumInfo, albumImage);

            Integer albumYear = albumBundle.getAlbumInfo().getAlbumYear();
            albumTitle.setText(albumInfo.getName());
            albumDetails.setText(albumYear == null ? "🎵" : String.valueOf(albumYear));
            List<TrackRecord> albumsTracks = albumBundle.getAlbumTracks();

            Map<Optional<Integer>, List<TrackRecord>> groupedByDiscNumber = albumsTracks
                    .stream()
                    .collect(groupingBy(trackRecord -> ofNullable(trackRecord.getDiskNumber())));

            List<Integer> sortedDiskNumbers = groupedByDiscNumber
                    .keySet()
                    .stream()
                    .map(o -> o.orElse(null))
                    .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                    .toList();

            boolean showDiskNumber = groupedByDiscNumber.size() != 1;
            log.debug("{} For album {} got disc numbers {} ", showDiskNumber, albumInfo.getName(), groupedByDiscNumber.keySet());

            for (Integer diskNumber : sortedDiskNumbers) {
                LoadedFxml<LibraryArtistAlbumDiscController> loaded = fxmlLoader.load(
                        FxmlView.LIBRARY_ARTIST_ALBUM_DISC,
                        new ArtistAlbumDiscBundle(
                                showDiskNumber ? diskNumber : null,
                                groupedByDiscNumber.get(ofNullable(diskNumber)),
                                albumBundle.getTrackSelectionModel(),
                                albumInfo
                        )
                );
                discControllers.add(loaded.controller());
                albumDiscsVbox.getChildren().add(loaded.parent());
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
