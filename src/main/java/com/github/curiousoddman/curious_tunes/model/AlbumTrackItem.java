package com.github.curiousoddman.curious_tunes.model;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;

public record AlbumTrackItem(TrackRecord trackRecord, AlbumRecord trackAlbum) {
}
