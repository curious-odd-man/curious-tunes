package com.github.curiousoddman.curious_tunes.model;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Data
@RequiredArgsConstructor
public class PlaylistItem {
    private final TrackRecord trackRecord;

    private PlaylistItemStatus playlistItemStatus = PlaylistItemStatus.QUEUED;

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PlaylistItem that)) return false;

        return Objects.equals(trackRecord.getId(), that.trackRecord.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(trackRecord.getId());
    }
}
