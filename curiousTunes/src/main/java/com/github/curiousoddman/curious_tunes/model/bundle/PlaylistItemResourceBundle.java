package com.github.curiousoddman.curious_tunes.model.bundle;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class PlaylistItemResourceBundle extends ListResourceBundle {
    private final String artist;
    private final AlbumRecord albumRecord;
    private final TrackRecord trackRecord;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"artist", artist},
                {"albumRecord", albumRecord},
                {"trackRecord", trackRecord},
        };
    }
}
