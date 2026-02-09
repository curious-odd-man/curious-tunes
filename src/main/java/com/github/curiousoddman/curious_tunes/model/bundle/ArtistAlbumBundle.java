package com.github.curiousoddman.curious_tunes.model.bundle;

import com.github.curiousoddman.curious_tunes.model.TrackSelectionModel;
import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class ArtistAlbumBundle extends ListResourceBundle {
    private final String artist;
    private final AlbumInfo albumInfo;
    private final TrackSelectionModel trackSelectionModel;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"artist", artist},
                {"albumInfo", albumInfo},
                {"trackRecordSelectionModel", trackSelectionModel}
        };
    }
}
