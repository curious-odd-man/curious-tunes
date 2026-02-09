package com.github.curiousoddman.curious_tunes.config;

import com.github.curiousoddman.curious_tunes.controller.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FxmlView<T> {
    public static final FxmlView<LibraryController> LIBRARY = new FxmlView<>(".\\fxml\\library.fxml", LibraryController.class);
    public static final FxmlView<LibraryArtistController> LIBRARY_ARTIST_ITEM = new FxmlView<>(".\\fxml\\library-artist.fxml", LibraryArtistController.class);
    public static final FxmlView<LibraryArtistAlbumController> LIBRARY_ARTIST_ALBUM = new FxmlView<>(".\\fxml\\library-artist-album.fxml", LibraryArtistAlbumController.class);
    public static final FxmlView<RescanLibraryController> RESCAN_MODAL = new FxmlView<>(".\\fxml\\rescan-modal.fxml", RescanLibraryController.class);
    public static final FxmlView<LibraryPlaylistController> LIBRARY_PLAYLIST = new FxmlView<>(".\\fxml\\library-playlist.fxml", LibraryPlaylistController.class);
    public static final FxmlView<PlaylistItemController> PLAYLIST_ITEM = new FxmlView<>(".\\fxml\\playlist-item.fxml", PlaylistItemController.class);
    public static final FxmlView<LibraryHistoryTabController> LIBRARY_TAB_HISTORY = new FxmlView<>("\\fxml\\library-tab-history.fxml", LibraryHistoryTabController.class);
    public static final FxmlView<LibraryLyricsTabController> LIBRARY_TAB_LYRICS = new FxmlView<>("\\fxml\\library-tab-lyrics.fxml", LibraryLyricsTabController.class);
    public static final FxmlView<LibraryTagEditTabController> LIBRARY_TAB_TAGS_EDIT = new FxmlView<>("\\fxml\\library-tab-edit-tags.fxml", LibraryTagEditTabController.class);
    public static final FxmlView<LibraryArtistAlbumTrackController> LIBRARY_ALBUM_TRACK = new FxmlView<>("\\fxml\\library-album-track.fxml", LibraryArtistAlbumTrackController.class);

    private final String fxmlPath;
    private final Class<T> controllerClass;
}
