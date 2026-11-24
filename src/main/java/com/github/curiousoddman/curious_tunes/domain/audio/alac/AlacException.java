package com.github.curiousoddman.curious_tunes.domain.audio.alac;

/**
 * An ALAC file or packet that cannot be decoded.
 */
public class AlacException extends RuntimeException {
    public AlacException(String message) {
        super(message);
    }
}
