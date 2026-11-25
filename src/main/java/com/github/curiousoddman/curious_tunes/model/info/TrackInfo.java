package com.github.curiousoddman.curious_tunes.model.info;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TrackInfo {
    protected final TrackRecord  trackRecord;
    protected final ArtistRecord trackArtist;
    protected final AlbumRecord  trackAlbum;

    // ============================================================
    // TrackRecord Delegates
    // ============================================================

    public Integer getTrackId() {
        return trackRecord.getId();
    }

    public String getTitle() {
        return trackRecord.getTitle();
    }

    public Integer getTrackNumber() {
        return trackRecord.getTrackNumber();
    }

    public String getReleaseDate() {
        return trackRecord.getReleaseDate();
    }

    public Integer getDiskNumber() {
        return trackRecord.getDiskNumber();
    }

    public String getGenre() {
        return trackRecord.getGenre();
    }

    public String getComposer() {
        return trackRecord.getComposer();
    }

    public String getFileLocation() {
        return trackRecord.getFileLocation();
    }

    public Long getDuration() {
        return trackRecord.getDuration();
    }

    public String getLyrics() {
        return trackRecord.getLyrics();
    }

    public void setLyrics(String value) {
        trackRecord.setLyrics(value);
    }

    // ============================================================
    // ArtistRecord Delegates
    // ============================================================

    public Integer getArtistId() {
        return trackArtist.getId();
    }

    public String getArtistName() {
        return trackArtist.getName();
    }

    // ============================================================
    // AlbumRecord Delegates
    // ============================================================

    public Integer getAlbumId() {
        return trackAlbum.getId();
    }

    public String getAlbumName() {
        return trackAlbum.getName();
    }

    public byte[] getAlbumImage() {
        return trackAlbum.getImage();
    }
}
