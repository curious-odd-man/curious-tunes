package com.github.curiousoddman.curious_tunes.model.bundle;

import com.github.curiousoddman.curious_tunes.model.PlaylistItem;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.model.PlaylistModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class PlaylistItemResourceBundle extends ListResourceBundle {
    private final String artist;
    private final AlbumRecord albumRecord;
    private final PlaylistItem playlistItem;
    private final PlaylistModel playlistModel;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"artist", artist},
                {"albumRecord", albumRecord},
                {"playlistItem", playlistItem},
                {"playlistModel", playlistModel}
        };
    }
}
