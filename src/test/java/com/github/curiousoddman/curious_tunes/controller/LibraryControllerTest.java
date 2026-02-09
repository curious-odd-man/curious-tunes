package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.test.TestApp;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

@Slf4j
class LibraryControllerTest {

    @Test
    void playM4aTest() {
        TestApp testApp = new TestApp();
        testApp.setAfterStartup(() -> {
            log.info("Starting test....");
            Media media = new javafx.scene.media.Media(Path.of("D:\\iTunes\\iTunes 1\\iTunes Media\\Music\\ABBA\\ABBA\\01 Mamma Mia.m4a").toUri().toString());
            MediaPlayer player = new MediaPlayer(media);
            player.statusProperty().addListener((observable, oldValue, newValue) ->
                    log.info("playback status: {} ", newValue));
            player.play();
        });
        testApp.launch();
    }
}