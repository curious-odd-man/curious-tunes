package com.github.curiousoddman.curious_tunes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Album {
    private String name;
    private String image;
    private List<Track> tracks;
}
