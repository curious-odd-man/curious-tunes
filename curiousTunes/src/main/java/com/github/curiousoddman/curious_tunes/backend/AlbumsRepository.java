package com.github.curiousoddman.curious_tunes.backend;

import com.github.curiousoddman.curious_tunes.model.Album;
import com.github.curiousoddman.curious_tunes.util.populate.PopulatePojo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AlbumsRepository {

    private static final String IMG_PATH = "C:\\Users\\curious\\Pictures\\Desktop Backgrounds\\Image %d.jpg";

    public List<Album> getAlbumsForArtist(String artist) {
        List<Album> albums = switch (artist) {
            case "ABBA" -> List.of(
                    PopulatePojo.populatePojo(new Album()),
                    PopulatePojo.populatePojo(new Album()));
            case "Adele" -> List.of(
                    PopulatePojo.populatePojo(new Album()),
                    PopulatePojo.populatePojo(new Album()),
                    PopulatePojo.populatePojo(new Album()));
            default -> throw new NoSuchElementException();
        };

        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        return albums
                .stream()
                .peek(a -> a.setImage(IMG_PATH.formatted(threadLocalRandom.nextInt(3) + 1)))
                .toList();
    }
}
