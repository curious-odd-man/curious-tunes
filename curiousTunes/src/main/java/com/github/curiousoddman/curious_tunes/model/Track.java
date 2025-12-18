package com.github.curiousoddman.curious_tunes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Track {
    private String name;
    private int duration;
    private String path;
}
