package com.github.curiousoddman.curious_tunes.model.bundle;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class ArtistAlbumBundle extends ListResourceBundle {
    private final String artist;
    private final AlbumRecord albumRecord;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"artist", artist},
                {"albumRecord", albumRecord}
        };
    }
}
