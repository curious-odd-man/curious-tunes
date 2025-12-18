package com.github.curiousoddman.curious_tunes.model.bundle;

import com.github.curiousoddman.curious_tunes.model.Album;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class ArtistAlbumBundle extends ListResourceBundle {
    private final String artist;
    private final Album album;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"artist", artist},
                {"album", album}
        };
    }
}
