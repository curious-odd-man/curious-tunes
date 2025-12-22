package com.github.curiousoddman.curious_tunes.model.bundle;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.model.ArtistSelectionModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class ArtistItemBundle extends ListResourceBundle {
    private final ArtistRecord artist;
    private final ArtistSelectionModel artistSelectionModel;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"artist", artist},
                {"artistSelectionModel", artistSelectionModel},
        };
    }
}

