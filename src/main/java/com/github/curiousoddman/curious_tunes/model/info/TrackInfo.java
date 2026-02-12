package com.github.curiousoddman.curious_tunes.model.info;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TrackInfo {
    protected final TrackRecord trackRecord;
    protected final ArtistRecord trackArtist;
    protected final AlbumRecord trackAlbum;

    // ============================================================
    // TrackRecord Delegates
    // ============================================================

    public Integer getTrackId() {
        return trackRecord.getId();
    }

    public void setTrackId(Integer value) {
        trackRecord.setId(value);
    }

    public Integer getFkAlbum() {
        return trackRecord.getFkAlbum();
    }

    public void setFkAlbum(Integer value) {
        trackRecord.setFkAlbum(value);
    }

    public String getTitle() {
        return trackRecord.getTitle();
    }

    public void setTitle(String value) {
        trackRecord.setTitle(value);
    }

    public Integer getTrackNumber() {
        return trackRecord.getTrackNumber();
    }

    public void setTrackNumber(Integer value) {
        trackRecord.setTrackNumber(value);
    }

    public String getReleaseDate() {
        return trackRecord.getReleaseDate();
    }

    public void setReleaseDate(String value) {
        trackRecord.setReleaseDate(value);
    }

    public Integer getDiskNumber() {
        return trackRecord.getDiskNumber();
    }

    public void setDiskNumber(Integer value) {
        trackRecord.setDiskNumber(value);
    }

    public Integer getAudioSampleRate() {
        return trackRecord.getAudioSampleRate();
    }

    public void setAudioSampleRate(Integer value) {
        trackRecord.setAudioSampleRate(value);
    }

    public String getGenre() {
        return trackRecord.getGenre();
    }

    public void setGenre(String value) {
        trackRecord.setGenre(value);
    }

    public String getComposer() {
        return trackRecord.getComposer();
    }

    public void setComposer(String value) {
        trackRecord.setComposer(value);
    }

    public String getFileLocation() {
        return trackRecord.getFileLocation();
    }

    public void setFileLocation(String value) {
        trackRecord.setFileLocation(value);
    }

    public Long getDuration() {
        return trackRecord.getDuration();
    }

    public void setDuration(Long value) {
        trackRecord.setDuration(value);
    }

    public String getStatus() {
        return trackRecord.getStatus();
    }

    public void setStatus(String value) {
        trackRecord.setStatus(value);
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

    public void setArtistId(Integer value) {
        trackArtist.setId(value);
    }

    public String getArtistName() {
        return trackArtist.getName();
    }

    public void setArtistName(String value) {
        trackArtist.setName(value);
    }

    public byte[] getArtistIcon() {
        return trackArtist.getIcon();
    }

    public void setArtistIcon(byte[] value) {
        trackArtist.setIcon(value);
    }

    // ============================================================
    // AlbumRecord Delegates
    // ============================================================

    public Integer getAlbumId() {
        return trackAlbum.getId();
    }

    public void setAlbumId(Integer value) {
        trackAlbum.setId(value);
    }

    public Integer getAlbumFkArtist() {
        return trackAlbum.getFkArtist();
    }

    public void setAlbumFkArtist(Integer value) {
        trackAlbum.setFkArtist(value);
    }

    public String getAlbumName() {
        return trackAlbum.getName();
    }

    public void setAlbumName(String value) {
        trackAlbum.setName(value);
    }

    public byte[] getAlbumImage() {
        return trackAlbum.getImage();
    }

    public void setAlbumImage(byte[] value) {
        trackAlbum.setImage(value);
    }

    public Byte getAlbumImageType() {
        return trackAlbum.getImageType();
    }

    public void setAlbumImageType(Byte value) {
        trackAlbum.setImageType(value);
    }
}
