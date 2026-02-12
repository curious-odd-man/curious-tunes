package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.backend.lyrics.LyricsService;
import com.github.curiousoddman.curious_tunes.backend.tags.MetadataTags;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.util.JooqUtils;
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
import org.jooq.Field;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;
import static com.github.curiousoddman.curious_tunes.util.ConversionUtils.str;
import static com.github.curiousoddman.curious_tunes.util.ConversionUtils.toInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryTagEditTabController implements Initializable {
    private final LyricsService lyricsService;
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

    private final DataAccess dataAccess;

    private MetadataTags fileTags;
    private TrackInfo trackInfo;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void showTags(MetadataTags fileTags, TrackInfo trackInfo) {
        this.fileTags = fileTags;
        this.trackInfo = trackInfo;

        fillFieldsFromDb();
        markDifferentFields(trackInfo);
    }

    @FXML
    public void onSyncDatabase(ActionEvent actionEvent) {
        fillFieldsFromFile();
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

    @FXML
    @SneakyThrows
    public void onSave(ActionEvent actionEvent) {
        applyFieldsToDb();
        fileTags.updateFile();
    }

    private void applyFieldsToDb() {
        log.info("Updating track info \n{}", trackInfo);
        ArtistRecord artist = trackInfo.getTrackArtist();
        if (!artistField.getText().equals(trackInfo.getArtistName())) {
            log.info("Artist name changed!");
            artist = dataAccess.getOrInsertArtist(artistField.getText());
            AlbumRecord album = dataAccess.getOrInsertAlbum(artist.getId(), albumField.getText(), trackInfo.getAlbumImage());
            TrackRecord trackRecord = trackInfo.getTrackRecord();
            trackRecord.setFkAlbum(album.getId());
            trackRecord.update(TRACK.FK_ALBUM);
        } else if (!albumField.getText().equals(trackInfo.getAlbumName())) {
            log.info("Album changed");
            AlbumRecord album = dataAccess.getOrInsertAlbum(artist.getId(), albumField.getText(), trackInfo.getAlbumImage());
            TrackRecord trackRecord = trackInfo.getTrackRecord();
            trackRecord.setFkAlbum(album.getId());
            trackRecord.update(TRACK.FK_ALBUM);
        }

        List<Field<?>> fieldsToUpdate = new ArrayList<>();

        JooqUtils.updateIfChanged(trackInfo.getTitle(), fileTags.getTitle(), trackInfo::setTitle, fieldsToUpdate, TRACK.TITLE);
        JooqUtils.updateIfChanged(str(trackInfo.getTrackNumber()), str(fileTags.getTrackNumber()), v -> trackInfo.setTrackNumber(toInteger(v)), fieldsToUpdate, TRACK.TRACK_NUMBER);
        JooqUtils.updateIfChanged(str(trackInfo.getDiskNumber()), str(fileTags.getDiskNumber()), v -> trackInfo.setDiskNumber(toInteger(v)), fieldsToUpdate, TRACK.DISK_NUMBER);
        JooqUtils.updateIfChanged(trackInfo.getGenre(), fileTags.getGenre(), trackInfo::setGenre, fieldsToUpdate, TRACK.GENRE);
        JooqUtils.updateIfChanged(trackInfo.getComposer(), fileTags.getComposer(), trackInfo::setComposer, fieldsToUpdate, TRACK.COMPOSER);
        JooqUtils.updateIfChanged(trackInfo.getReleaseDate(), fileTags.getReleaseDate(), trackInfo::setReleaseDate, fieldsToUpdate, TRACK.RELEASE_DATE);
        JooqUtils.updateIfChanged(trackInfo.getLyrics(), fileTags.getLyrics(), trackInfo::setLyrics, fieldsToUpdate, TRACK.LYRICS);

        if (!fieldsToUpdate.isEmpty()) {
            log.info("Updating \n{}", trackInfo);
            trackInfo.getTrackRecord().update(fieldsToUpdate);
        }
    }

    /*    private void applyFieldsToFileTags() {
        fileTags.setArtist(artistField.getText());
        fileTags.setAlbum(albumField.getText());
        fileTags.setTitle(titleField.getText());
        setIfDefined(fileTags::setTrackNumber, trackNumberField.getText(), this::parseInt);
        setIfDefined(fileTags::setDiskNumber, diskNumberField.getText(), this::parseInt);
        fileTags.setGenre(genreField.getText());
        fileTags.setComposer(composerField.getText());
        fileTags.setReleaseDate(releaseDateField.getText());
        fileTags.setLyrics(lyricsEditArea.getText());
    }*/

    private void fillFieldsFromDb() {
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

    private void fillFieldsFromFile() {
        artistField.setText(fileTags.getArtist());
        albumField.setText(fileTags.getAlbum());
        titleField.setText(fileTags.getTitle());
        trackNumberField.setText(str(fileTags.getTrackNumber()));
        diskNumberField.setText(str(fileTags.getDiskNumber()));
        genreField.setText(fileTags.getGenre());
        composerField.setText(fileTags.getComposer());
        releaseDateField.setText(fileTags.getReleaseDate());
        lyricsEditArea.setText(fileTags.getLyrics());
    }

    private void markDifferentFields(TrackInfo trackInfo) {
        markIfDifferent(artistField, fileTags.getArtist(), trackInfo.getArtistName());
        markIfDifferent(albumField, fileTags.getAlbum(), trackInfo.getAlbumName());
        markIfDifferent(titleField, fileTags.getTitle(), trackInfo.getTitle());
        markIfDifferent(trackNumberField, fileTags.getTrackNumber(), trackInfo.getTrackNumber());
        markIfDifferent(diskNumberField, fileTags.getDiskNumber(), trackInfo.getDiskNumber());
        markIfDifferent(genreField, fileTags.getGenre(), trackInfo.getGenre());
        markIfDifferent(composerField, fileTags.getComposer(), trackInfo.getComposer());
        markIfDifferent(releaseDateField, fileTags.getReleaseDate(), trackInfo.getReleaseDate());
        markIfDifferent(lyricsEditArea, fileTags.getLyrics(), trackInfo.getLyrics());
    }

    private void markIfDifferent(TextInputControl field, Object fileValue, Object dbValue) {
        boolean different = !Objects.equals(fileValue, dbValue);
        if (different) {
            field.setStyle("-fx-border-color: orange; -fx-border-width: 2;");
        } else {
            field.setStyle(null);
        }
    }
}
