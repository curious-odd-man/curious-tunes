package com.github.curiousoddman.curious_tunes.model.bundle;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.TrackSelectionModel;
import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class ArtistAlbumDiscBundle extends ListResourceBundle {
    private final Integer discNumber;
    private final List<TrackRecord> trackRecords;
    private final TrackSelectionModel trackSelectionModel;
    private final AlbumInfo albumInfo;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"discNumber", discNumber},
                {"trackRecords", trackRecords},
                {"trackRecordSelectionModel", trackSelectionModel},
                {"albumInfo", albumInfo}
        };
    }
}
