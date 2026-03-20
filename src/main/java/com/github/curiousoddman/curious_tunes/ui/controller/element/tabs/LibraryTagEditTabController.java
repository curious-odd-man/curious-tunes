package com.github.curiousoddman.curious_tunes.ui.controller.element.tabs;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackOverridesHistoryRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.domain.lyrics.LyricsService;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.util.ConversionUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.TableField;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;
import static com.github.curiousoddman.curious_tunes.util.ConversionUtils.str;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryTagEditTabController implements Initializable {
    @FXML
    public GridPane tagsGrid;
    @FXML
    public TextField artistField;
    @FXML
    public TextField albumField;
    @FXML
    public TextField titleField;
    @FXML
    public TextField trackNumberField;
    @FXML
    public TextField releaseDateField;
    @FXML
    public TextField diskNumberField;
    @FXML
    public TextField genreField;
    @FXML
    public TextField composerField;
    @FXML
    public ImageView albumCoverImage;
    @FXML
    public TextArea lyricsEditArea;
    @FXML
    public AnchorPane rootPane;

    private final LyricsService lyricsService;
    private final DataAccess dataAccess;

    private TrackInfo trackInfo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void showTags(TrackInfo trackInfo) {
        this.trackInfo = trackInfo;

        fillFieldsFromTrackInfo();
        markModifiedFields();
    }

    @FXML
    public void onSyncDatabase(ActionEvent actionEvent) {
        log.error("Not implemented");
    }

    @FXML
    public void onFindLyrics(ActionEvent actionEvent) {
        lyricsService.findLyricsAsync(
                trackInfo.getTrackArtist().getName(),
                trackInfo.getTrackAlbum().getName(),
                trackInfo.getTitle(),
                lyrics -> Platform.runLater(() -> lyricsEditArea.setText(lyrics)),
                () -> log.error("Unable to find lyrics online")
        );
    }

    record ChangeCheck<T>(T trackValue, String uiValue, TableField<TrackRecord, T> field, Function<String, T> mapper) {

    }

    @FXML
    @SneakyThrows
    public void onSave(ActionEvent actionEvent) {
        log.info("Updating track info \n{}", trackInfo);
        TrackRecord trackRecord = trackInfo.getTrackRecord();

        ArtistRecord artist = trackInfo.getTrackArtist();
        if (!artistField.getText().equals(trackInfo.getArtistName())) {
            log.info("Artist name changed!");
            artist = dataAccess.getOrInsertArtist(artistField.getText());
            AlbumRecord album = dataAccess.getOrInsertAlbum(artist.getId(), albumField.getText(), trackInfo.getAlbumImage());
            dataAccess.storeTrackOverride(trackInfo.getTrackRecord(), TRACK.FK_ALBUM, str(trackRecord.getFkAlbum()));
            trackRecord.setFkAlbum(album.getId());
            trackRecord.update(TRACK.FK_ALBUM);
        } else if (!albumField.getText().equals(trackInfo.getAlbumName())) {
            log.info("Album changed");
            AlbumRecord album = dataAccess.getOrInsertAlbum(artist.getId(), albumField.getText(), trackInfo.getAlbumImage());
            dataAccess.storeTrackOverride(trackInfo.getTrackRecord(), TRACK.FK_ALBUM, str(trackRecord.getFkAlbum()));
            trackRecord.setFkAlbum(album.getId());
            trackRecord.update(TRACK.FK_ALBUM);
        }

        List<ChangeCheck<?>> changeChecks = List.of(
                new ChangeCheck<>(trackInfo.getTitle(), titleField.getText(), TRACK.TITLE, Function.identity()),
                new ChangeCheck<>(trackInfo.getTrackNumber(), trackNumberField.getText(), TRACK.TRACK_NUMBER, ConversionUtils::toInteger),
                new ChangeCheck<>(trackInfo.getDiskNumber(), diskNumberField.getText(), TRACK.DISK_NUMBER, ConversionUtils::toInteger),
                new ChangeCheck<>(trackInfo.getGenre(), genreField.getText(), TRACK.GENRE, Function.identity()),
                new ChangeCheck<>(trackInfo.getComposer(), composerField.getText(), TRACK.COMPOSER, Function.identity()),
                new ChangeCheck<>(trackInfo.getReleaseDate(), releaseDateField.getText(), TRACK.RELEASE_DATE, Function.identity()),
                new ChangeCheck<>(trackInfo.getLyrics(), lyricsEditArea.getText(), TRACK.LYRICS, Function.identity())
        );

        for (ChangeCheck changeCheck : changeChecks) {
            if (!Objects.equals(changeCheck.uiValue, str(changeCheck.trackValue))) {
                log.info("Value for '{}' does not match '{}' --> '{}', updating...", changeCheck.field.getName(), changeCheck.uiValue, changeCheck.trackValue);
                dataAccess.storeTrackOverride(trackInfo.getTrackRecord(), changeCheck.field, str(changeCheck.trackValue));
                trackRecord.set(changeCheck.field, changeCheck.mapper.apply(changeCheck.uiValue));
                trackRecord.update(changeCheck.field);
            }
        }
        markModifiedFields();
    }

    private void fillFieldsFromTrackInfo() {
        artistField.setText(trackInfo.getArtistName());
        albumField.setText(trackInfo.getAlbumName());
        titleField.setText(trackInfo.getTitle());
        trackNumberField.setText(str(trackInfo.getTrackNumber()));
        diskNumberField.setText(str(trackInfo.getDiskNumber()));
        genreField.setText(trackInfo.getGenre());
        composerField.setText(trackInfo.getComposer());
        releaseDateField.setText(trackInfo.getReleaseDate());
        lyricsEditArea.setText(trackInfo.getLyrics());
    }

    private void markModifiedFields() {
        List<TrackOverridesHistoryRecord> trackOverrides = dataAccess.getTrackOverrides(trackInfo.getTrackId());

        // How to detect?
        //markIfDifferent(artistField, trackInfo.getArtistId(), trackOverrides, TRACK.FK_ALBUM);
        markIfDifferent(albumField, trackInfo.getAlbumId(), trackOverrides, TRACK.FK_ALBUM);
        markIfDifferent(titleField, trackInfo.getTitle(), trackOverrides, TRACK.TITLE);
        markIfDifferent(trackNumberField, trackInfo.getTrackNumber(), trackOverrides, TRACK.TRACK_NUMBER);
        markIfDifferent(diskNumberField, trackInfo.getDiskNumber(), trackOverrides, TRACK.DISK_NUMBER);
        markIfDifferent(genreField, trackInfo.getGenre(), trackOverrides, TRACK.GENRE);
        markIfDifferent(composerField, trackInfo.getComposer(), trackOverrides, TRACK.COMPOSER);
        markIfDifferent(releaseDateField, trackInfo.getReleaseDate(), trackOverrides, TRACK.RELEASE_DATE);
        markIfDifferent(lyricsEditArea, trackInfo.getLyrics(), trackOverrides, TRACK.LYRICS);
    }

    private void markIfDifferent(TextInputControl inputField,
                                 Object currentValue,
                                 List<TrackOverridesHistoryRecord> trackOverrides,
                                 TableField<TrackRecord, ?> field) {
        boolean wasOverridden = wasOverridden(currentValue, trackOverrides, field);
        if (wasOverridden) {
            inputField.setStyle("-fx-border-color: orange; -fx-border-width: 2;");
        } else {
            inputField.setStyle(null);
        }
    }

    public static boolean wasOverridden(Object currentValue, List<TrackOverridesHistoryRecord> trackOverrides, TableField<TrackRecord, ?> field) {
        Optional<TrackOverridesHistoryRecord> override = trackOverrides
                .reversed()
                .stream()
                .filter(r -> r.getField().equals(field.getName()))
                .findFirst();
        return override.isPresent()
                && !Objects.equals(currentValue, override.get().getOldValue());
    }
}
