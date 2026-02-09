package com.github.curiousoddman.curious_tunes.model.bundle;

import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.model.TrackSelectionModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class ArtistAlbumTrackBundle extends ListResourceBundle {
    private final TrackInfo trackInfo;
    private final TrackSelectionModel trackSelectionModel;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"trackInfo", trackInfo},
                {"trackRecordSelectionModel", trackSelectionModel}
        };
    }
}
