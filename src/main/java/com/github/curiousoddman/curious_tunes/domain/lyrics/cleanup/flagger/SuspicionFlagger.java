package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.flagger;

public interface SuspicionFlagger {
    boolean test(String lyrics);

    Flag flag();
}
