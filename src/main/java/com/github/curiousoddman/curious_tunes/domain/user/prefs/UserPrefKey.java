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

    // ── UI layout ───────────────────────────────────────────────────────────
    ARTISTS_SPLIT_WIDTH("ui.split.artists"),         // divider position (px)
    PLAYLIST_SPLIT_WIDTH("ui.split.playlist"),        // divider position (px)

    // ── Session ─────────────────────────────────────────────────────────────
    LAST_PLAYLIST_ID("session.last.playlist.id"); // FK → playlists.id

    private final String key;
}