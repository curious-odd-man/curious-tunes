package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.PlaybackHistoryRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import javafx.collections.FXCollections;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.curiousoddman.curious_tunes.util.TimeUtils.secondsToHumanTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryHistoryTabController implements Initializable {
    private final DataAccess dataAccess;

    public Label totalTimeListened;
    public Label songsPlayed;
    public Label albumsPlayed;
    public Label artistsPlayed;
    public TableView<HistoryTableRow> latestSongsTable;
    public TableColumn<HistoryTableRow, String> dateColumn;
    public TableColumn<HistoryTableRow, String> timeColumn;
    public TableColumn<HistoryTableRow, String> songColumn;
    public TableColumn<HistoryTableRow, String> albumColumn;
    public TableColumn<HistoryTableRow, String> artistColumn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        songColumn.setCellValueFactory(new PropertyValueFactory<>("song"));
        albumColumn.setCellValueFactory(new PropertyValueFactory<>("album"));
        artistColumn.setCellValueFactory(new PropertyValueFactory<>("artist"));
        renewStats();
    }

    public void renewStats() {
        Instant start = Instant.now();
        List<PlaybackHistoryRecord> allHistoryRecords = dataAccess.getAllHistoryRecords();
        log.info("Returned {} history rows in {} ms.", allHistoryRecords.size(), Duration.between(start, Instant.now()).toMillis());

        List<Integer> allTracksFks = allHistoryRecords.stream().map(PlaybackHistoryRecord::getFkTrack).toList();

        start = Instant.now();
        Map<Integer, TrackRecord> tracksById = dataAccess
                .getTracks(allTracksFks)
                .stream()
                .collect(Collectors.toMap(
                        TrackRecord::getId,
                        Function.identity()
                ));
        log.info("Returned {} matched track rows in {} ms.", tracksById.size(), Duration.between(start, Instant.now()).toMillis());

        Long totalDuration = allTracksFks.stream().map(tracksById::get).map(TrackRecord::getDuration).reduce(0L, Long::sum);
        totalTimeListened.setText(secondsToHumanTime(totalDuration));

        List<HistoryTableRow> historyTableRows = allHistoryRecords
                .stream()
                //.limit(100) // TODO: when history is too long
                .map(historyRecord -> {
                    TrackRecord trackRecord = tracksById.get(historyRecord.getFkTrack());
                    AlbumRecord albumRecord = dataAccess.getAlbum(trackRecord.getFkAlbum());
                    ArtistRecord artistRecord = dataAccess.getArtist(albumRecord.getFkArtist());
                    return new HistoryTableRow(
                            historyRecord.getDatetime().toLocalDate().toString(),
                            historyRecord.getDatetime().toLocalTime().toString(),
                            trackRecord.getTitle(),
                            albumRecord.getName(),
                            artistRecord.getName()
                    );
                })
                .toList();

        latestSongsTable.setItems(FXCollections.observableList(historyTableRows));
        latestSongsTable.refresh();
    }

    @Getter
    @AllArgsConstructor
    public static class HistoryTableRow {
        private final String date;
        private final String time;
        private final String song;
        private final String album;
        private final String artist;
    }
}
