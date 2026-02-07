package com.github.curiousoddman.curious_tunes.model.bundle;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class ArtistAlbumTrackBundle extends ListResourceBundle {
    private final TrackRecord trackRecord;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"trackRecord", trackRecord}
        };
    }
}
