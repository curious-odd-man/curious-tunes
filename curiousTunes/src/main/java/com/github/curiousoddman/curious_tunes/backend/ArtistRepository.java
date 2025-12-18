package com.github.curiousoddman.curious_tunes.backend;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArtistRepository {
    public List<String> getArtists() {
        return List.of(
                "ABBA",
                "Adele",
                "Opeth",
                "Death",
                "Nile"
        );
    }
}
