package com.github.curiousoddman.curious_tunes.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FxmlView {
    LIBRARY(".\\fxml\\library.fxml"),
    LIBRARY_ARTIST_ITEM(".\\fxml\\library-artist.fxml"),
    LIBRARY_ARTIST_ALBUM(".\\fxml\\library-artist-album.fxml"),
    RESCAN_MODAL(".\\fxml\\rescan-modal.fxml"),
    LIBRARY_PLAYLIST(".\\fxml\\library-playlist.fxml"),
    PLAYLIST_ITEM(".\\fxml\\playlist-item.fxml")
    ;

    private final String fxmlPath;
}
