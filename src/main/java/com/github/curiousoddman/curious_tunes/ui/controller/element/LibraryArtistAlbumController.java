package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.model.LibraryDragPayload;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumDiscBundle;
import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistAddMode;
import com.github.curiousoddman.curious_tunes.util.ImageUtils;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
    private final FxmlLoader                fxmlLoader;
    private final LibraryDragPayload        dragPayload;
    private final UserPreferencesService    userPreferencesService;
    public        ImageView                 albumImage;
    public        Label                     albumTitle;
    public        Label                     albumDetails;
    public        VBox                      albumDiscsVbox;
    public        BorderPane                pane;
    public        ImageView                 playImageButton;

    private final List<LibraryArtistAlbumDiscController> discControllers = new ArrayList<>();
    private       AlbumInfo                              albumInfo;
    private       FadeTransition                         playFade;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        if (resources instanceof ArtistAlbumBundle albumBundle) {
            albumInfo = albumBundle.getAlbumInfo();
            ImageUtils.setImageIfPresent(albumInfo, albumImage);
            initDrag();

            Integer albumYear = albumBundle.getAlbumInfo()
                                           .getAlbumYear();
            albumTitle.setText(albumInfo.getName());
            List<TrackRecord> albumsTracks = albumBundle.getAlbumTracks();
            albumDetails.setText(buildDetails(albumYear, albumsTracks));

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
                albumDiscsVbox.getChildren()
                              .add(loaded.parent());
            }
        }
    }

    /**
     * "1999 · 12 tracks · 48:12 · Rock"
     */
    private static String buildDetails(Integer year, List<TrackRecord> tracks) {
        List<String> parts = new ArrayList<>();
        if (year != null) {
            parts.add(String.valueOf(year));
        }
        parts.add(tracks.size() + (tracks.size() == 1 ? " track" : " tracks"));
        long totalSeconds = tracks.stream()
                                  .map(TrackRecord::getDuration)
                                  .filter(Objects::nonNull)
                                  .mapToLong(Long::longValue)
                                  .sum();
        if (totalSeconds > 0) {
            parts.add(TimeUtils.secondsToHumanTime((int) totalSeconds));
        }
        String genres = tracks.stream()
                              .map(TrackRecord::getGenre)
                              .filter(genre -> genre != null && !genre.isBlank())
                              .distinct()
                              .limit(3)
                              .collect(Collectors.joining(", "));
        if (!genres.isEmpty()) {
            parts.add(genres);
        }
        return String.join(" · ", parts);
    }

    public void onAlbumImageHover(MouseEvent mouseEvent) {
        fadePlayImageButtonTo(1);
    }

    public void onAlbumImageUnhover(MouseEvent mouseEvent) {
        fadePlayImageButtonTo(0);
    }

    public AlbumInfo getAlbumInfo() {
        return albumInfo;
    }

    /**
     * Size of the cover in the list view; the play overlay keeps its proportion.
     */
    public void setCoverSize(double size) {
        albumImage.setFitWidth(size);
        albumImage.setFitHeight(size);
        double overlay = Math.round(size * 0.57);
        playImageButton.setFitWidth(overlay);
        playImageButton.setFitHeight(overlay);
    }

    public void onPlayImageClicked(MouseEvent mouseEvent) {
        playAlbum();
    }

    /**
     * Replaces the queue with this album and starts playing.
     */
    public void playAlbum() {
        AddToPlaylistEvent event = AddToPlaylistEvent
                .builder()
                .source(this)
                .albums(List.of(albumInfo.getAlbumRecord()))
                .shuffle(userPreferencesService.isPlayShuffled() ? Shuffle.AFTER_ADDING_TO_PLAYLIST : Shuffle.SKIP)
                .playlistAddMode(PlaylistAddMode.REPLACE)
                .build();
        applicationEventPublisher.publishEvent(event);
    }

    @FXML
    public void onPlayButtonClicked(ActionEvent actionEvent) {
        playAlbum();
    }

    @FXML
    public void onAddToQueueClicked(ActionEvent actionEvent) {
        publish(PlaylistAddMode.APPEND, userPreferencesService.isPlayShuffled() ? Shuffle.BEFORE_ADDING_TO_PLAYLIST : Shuffle.SKIP);
    }

    @FXML
    public void onShuffleClicked(ActionEvent actionEvent) {
        publish(PlaylistAddMode.REPLACE, Shuffle.AFTER_ADDING_TO_PLAYLIST);
    }

    private void publish(PlaylistAddMode mode, Shuffle shuffle) {
        applicationEventPublisher.publishEvent(AddToPlaylistEvent
                .builder()
                .source(this)
                .albums(List.of(albumInfo.getAlbumRecord()))
                .shuffle(shuffle)
                .playlistAddMode(mode)
                .build());
    }

    private void fadePlayImageButtonTo(int value) {
        if (playFade != null) {
            playFade.stop();        // a quick in/out of the pointer must not start a second animation
        }
        playFade = new FadeTransition(Duration.millis(250), playImageButton);
        playFade.setFromValue(playImageButton.getOpacity());
        playFade.setToValue(value);
        playFade.play();
    }

    /**
     * The cover can be dragged into the queue.
     */
    private void initDrag() {
        Node dragHandle = albumImage.getParent();
        dragHandle.setOnDragDetected(event -> {
            Dragboard        dragboard = dragHandle.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content   = new ClipboardContent();
            content.putString(albumInfo.getName());
            dragboard.setContent(content);
            ImageView preview = new ImageView(albumImage.getImage());
            preview.setPreserveRatio(true);
            preview.setFitWidth(90);
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            dragboard.setDragView(preview.snapshot(parameters, null));
            dragPayload.setAlbums(List.of(albumInfo.getAlbumRecord()));
            event.consume();
        });
        dragHandle.setOnDragDone(event -> dragPayload.clear());
    }
}
