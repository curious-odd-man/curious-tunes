package com.github.curiousoddman.curious_tunes.model.bundle;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListResourceBundle;

@Getter
@RequiredArgsConstructor
public class ArtistItemBundle extends ListResourceBundle {
    private final String artist;
    private final String icon;

    @Override
    protected Object[][] getContents() {
        return new Object[][]{
                {"artist", artist},
                {"icon", icon}
        };
    }
}
