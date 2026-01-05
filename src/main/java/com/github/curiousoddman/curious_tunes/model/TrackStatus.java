package com.github.curiousoddman.curious_tunes.model;

public enum TrackStatus {
    ACTIVE,     // File is present and is used
    IGNORED,    // File is present, but user marked it as ignored
    DELETED,    // File was deleted using application
    MISSING     // File was deleted/moved outside of application
}
