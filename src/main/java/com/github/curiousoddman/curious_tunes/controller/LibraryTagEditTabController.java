package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.backend.tags.MetadataTags;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
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
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.util.ConversionUtils.setIfDefined;

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

    private final DataAccess dataAccess;

    private MetadataTags fileTags;
    private TrackInfo trackInfo;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void showTags(MetadataTags fileTags, TrackInfo trackInfo) {
        this.fileTags = fileTags;
        this.trackInfo = trackInfo;

        fillTextFieldsFromFile();
        markDifferentFields(trackInfo);
    }


    @FXML
    public void onSyncDatabase(ActionEvent actionEvent) {
        ArtistRecord artistRecord = dataAccess.getOrInsertArtist(fileTags.getArtist());
        AlbumRecord albumRecord = dataAccess.getOrInsertAlbum(artistRecord.getId(), fileTags.getAlbum(), fileTags.getAlbumCover().getData());
        TrackRecord trackRecord = dataAccess.getTrack(albumRecord.getId(), fileTags.getTitle());
        showTags(
                fileTags,
                new PlaylistItem(trackRecord, artistRecord, albumRecord)
        );
    }

    @FXML
    public void onFindLyrics(ActionEvent actionEvent) {
    }

    @FXML
    @SneakyThrows
    public void onSave(ActionEvent actionEvent) {
        applyFieldsToFileTags();
        fileTags.updateFile();
        onSyncDatabase(actionEvent);
    }

    private void applyFieldsToFileTags() {
        fileTags.setArtist(artistField.getText());
        fileTags.setAlbum(albumField.getText());
        fileTags.setTitle(titleField.getText());
        setIfDefined(fileTags::setTrackNumber, trackNumberField.getText(), this::parseInt);
        setIfDefined(fileTags::setDiskNumber, diskNumberField.getText(), this::parseInt);
        fileTags.setGenre(genreField.getText());
        fileTags.setComposer(composerField.getText());
        fileTags.setReleaseDate(releaseDateField.getText());
        fileTags.setLyrics(lyricsEditArea.getText());
    }

    private Integer parseInt(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toString(Integer value) {
        return value == null ? "" : value.toString();
    }


    private void fillTextFieldsFromFile() {
        artistField.setText(fileTags.getArtist());
        albumField.setText(fileTags.getAlbum());
        titleField.setText(fileTags.getTitle());
        trackNumberField.setText(toString(fileTags.getTrackNumber()));
        diskNumberField.setText(toString(fileTags.getDiskNumber()));
        genreField.setText(fileTags.getGenre());
        composerField.setText(fileTags.getComposer());
        releaseDateField.setText(fileTags.getReleaseDate());
        lyricsEditArea.setText(fileTags.getLyrics());
    }

    private void markDifferentFields(TrackInfo trackInfo) {
        ArtistRecord trackArtist = trackInfo.getTrackArtist();
        AlbumRecord trackAlbum = trackInfo.getTrackAlbum();
        TrackRecord trackRecord = trackInfo.getTrackRecord();
        markIfDifferent(artistField, fileTags.getArtist(), trackArtist.getName());
        markIfDifferent(albumField, fileTags.getAlbum(), trackAlbum.getName());
        markIfDifferent(titleField, fileTags.getTitle(), trackRecord.getTitle());
        markIfDifferent(trackNumberField, fileTags.getTrackNumber(), trackRecord.getTrackNumber());
        markIfDifferent(diskNumberField, fileTags.getDiskNumber(), trackRecord.getDiskNumber());
        markIfDifferent(genreField, fileTags.getGenre(), trackRecord.getGenre());
        markIfDifferent(composerField, fileTags.getComposer(), trackRecord.getComposer());
        markIfDifferent(releaseDateField, fileTags.getReleaseDate(), trackRecord.getReleaseDate());
        markIfDifferent(lyricsEditArea, fileTags.getLyrics(), trackRecord.getLyrics());
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
