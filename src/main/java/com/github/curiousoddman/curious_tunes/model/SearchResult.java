package com.github.curiousoddman.curious_tunes.model;

/**
 * One hit of the library search.
 *
 * @param kind     what was found
 * @param id       id of the artist, album or track
 * @param title    name of the artist, album or track
 * @param subtitle what to show next to it (the artist of an album, the album and artist of a track)
 * @param artistId artist of the hit
 * @param albumId  album of the hit (null for an artist)
 */
public record SearchResult(Kind kind, int id, String title, String subtitle, int artistId, Integer albumId) {
    public enum Kind {ARTIST, ALBUM, TRACK}
}
