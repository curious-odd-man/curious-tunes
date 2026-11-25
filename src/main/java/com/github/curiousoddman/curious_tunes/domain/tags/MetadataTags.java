package com.github.curiousoddman.curious_tunes.domain.tags;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;

/**
 * Tags read from an audio file. Read-only: edits from the tag editor and the lyrics tab are stored in the database.
 */
public interface MetadataTags {
    boolean updateTrackIfChanged(TrackRecord trackRecord, Integer newAlbumId);

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
}
