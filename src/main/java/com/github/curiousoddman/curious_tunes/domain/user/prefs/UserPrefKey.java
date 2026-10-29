package com.github.curiousoddman.curious_tunes.domain.user.prefs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserPrefKey {

    // ── Window ──────────────────────────────────────────────────────────────
    WINDOW_X("window.x"),
    WINDOW_Y("window.y"),
    WINDOW_WIDTH("window.width"),
    WINDOW_HEIGHT("window.height"),
    WINDOW_MAXIMIZED("window.maximized"),
    LYRICS_FONT_SIZE("lyrics.font.size"),

    // ── Playback ────────────────────────────────────────────────────────────
    VOLUME("playback.volume"),          // 0 – 100

    // ── Playback window (simple view only) ──────────────────────────────────
    PLAYBACK_WINDOW_X("playback.window.x"),
    PLAYBACK_WINDOW_Y("playback.window.y"),
    PLAYBACK_WINDOW_WIDTH("playback.window.width"),
    PLAYBACK_WINDOW_HEIGHT("playback.window.height"),
    PLAYBACK_LYRICS_OPEN("playback.lyrics.open"),         // lyrics drawer in the compact view
    PLAYBACK_LYRICS_AUTOFIT("playback.lyrics.autofit"),

    // ── UI layout ───────────────────────────────────────────────────────────
    ARTISTS_SPLIT_WIDTH("ui.split.artists"),         // divider position (px)
    PLAYLIST_SPLIT_WIDTH("ui.split.playlist"),        // divider position (px)
    ALBUMS_SORT("ui.albums.sort"),                    // AlbumSort name
    ALBUMS_GRID_VIEW("ui.albums.grid"),               // true = grid of covers, false = list
    ALBUMS_LIST_COVER_SIZE("ui.albums.list.cover"),   // cover size (px) in the list
    ALBUMS_GRID_COVER_SIZE("ui.albums.grid.cover"),   // cover size (px) in the grid

    // ── Library ─────────────────────────────────────────────────────────────
    LIBRARY_ROOT("library.root"),                    // last folder used for "Rescan library"

    // ── Session ─────────────────────────────────────────────────────────────
    LAST_PLAYLIST_ID("session.last.playlist.id"); // FK → playlists.id

    private final String key;
}