package com.github.curiousoddman.curious_tunes.backend.tags;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;

import java.io.IOException;

public interface MetadataTags {
    <T> T validate(String description, T field, T newValue);

    boolean updateTrackIfChanged(TrackRecord trackRecord);

    String getFileLocation();

    String getArtist();

    String getAlbum();

    String getTitle();

    Integer getTrackNumber();

    String getReleaseDate();

    Integer getDiskNumber();

    Integer getSampleRate();

    String getGenre();

    String getComposer();

    Long getDuration();

    String getLyrics();

    AlbumCover getAlbumCover();

    void setArtist(String artist);

    void setAlbum(String album);

    void setTitle(String title);

    void setTrackNumber(Integer trackNumber);

    void setReleaseDate(String releaseDate);

    void setDiskNumber(Integer diskNumber);

    void setGenre(String genre);

    void setComposer(String composer);

    void setLyrics(String lyrics);

    void setAlbumCover(AlbumCover albumCover);

    void updateFile() throws IOException;
}
