package com.github.curiousoddman.curious_tunes.actions.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLyricsPayload {
    private String lyrics;
    private Path path;
}
