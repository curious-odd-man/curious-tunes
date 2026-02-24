package com.github.curiousoddman.curious_tunes.domain.player;

import com.github.curiousoddman.curious_tunes.domain.MediaProvider;
import com.github.curiousoddman.curious_tunes.event.PlayPauseEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import com.github.curiousoddman.curious_tunes.model.PlaybackState;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistModel;
import com.github.curiousoddman.curious_tunes.ui.controller.custom.WaveformDataListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class AudioPlayer {
    private final ApplicationEventPublisher eventPublisher;
    private final PlaylistModel playlistModel;
    private final MediaProvider mediaProvider;
    private final MediaPlayerListeningTracker mediaPlayerListeningTracker;

    private MediaPlayer currentPlayer;
    private MediaPlayer nextPlayer;
    private PlaylistItem nextPlaylistItem;

    private DoubleProperty volumeProperty;
    private TrackPlaybackProgressListener playbackProgressListener;
    @Getter
    private final SimpleObjectProperty<PlaybackState> playbackStatusProperty;
    @Getter
    private final SimpleObjectProperty<PlaylistItem> playlistItemProperty;
    private WaveformDataListener waveformDataListener;

    public AudioPlayer(ApplicationEventPublisher eventPublisher,
                       PlaylistModel playlistModel,
                       MediaProvider mediaProvider,
                       MediaPlayerListeningTracker mediaPlayerListeningTracker) {
        this.eventPublisher = eventPublisher;
        this.playlistModel = playlistModel;
        this.mediaProvider = mediaProvider;
        this.mediaPlayerListeningTracker = mediaPlayerListeningTracker;

        playlistItemProperty = new SimpleObjectProperty<>();

        playbackStatusProperty = new SimpleObjectProperty<>(PlaybackState.STOPPED);
        playbackStatusProperty.addListener(ov -> {
            log.info("Current status: {}", playbackStatusProperty.get());
            eventPublisher.publishEvent(new PlayerStatusEvent(this, playbackStatusProperty.get(), playlistItemProperty.get()));
        });
    }

    public void linkWithUi(DoubleProperty volumeProperty,
                           TrackPlaybackProgressListener playbackProgressListener,
                           WaveformDataListener waveformDataListener) {
        this.volumeProperty = volumeProperty;
        this.playbackProgressListener = playbackProgressListener;
        this.waveformDataListener = waveformDataListener;
    }

    @SneakyThrows
    @EventListener
    public void onPlayPause(PlayPauseEvent playPauseEvent) {
        boolean isPlaying = playbackStatusProperty.get() == PlaybackState.PLAYING;
        boolean isPaused = playbackStatusProperty.get() == PlaybackState.PAUSED;
        log.info("onPlayPause: Currently {}", playbackStatusProperty.get());

        if (isPlaying) {
            log.info("Pausing...");
            currentPlayer.pause();
            playbackStatusProperty.set(PlaybackState.PAUSED);
            return;
        } else if (isPaused) {
            log.info("Resuming playback...");
            currentPlayer.play();
            playbackStatusProperty.set(PlaybackState.PLAYING);
            return;
        }

        if (isFirstSong()) {
            log.info("First song in playlist...");
            Optional<PlaylistItem> currentForPlayback = playlistModel.getNextForPlayback();
            if (currentForPlayback.isEmpty()) {
                log.info("No items to play");
                return;
            }
            playlistItemProperty.set(currentForPlayback.get());
            Media media = mediaProvider.getMedia(playlistItemProperty.get().getTrackRecord());
            currentPlayer = new MediaPlayer(media);
        } else {
            log.info("Not first song in playlist...");
            currentPlayer.dispose();
            currentPlayer = nextPlayer;
            playlistItemProperty.set(nextPlaylistItem);
            nextPlayer = null;
            nextPlaylistItem = null;
        }

        if (playlistItemProperty.get() == null) {
            log.info("No more songs to play in playlist");
            return;
        }

        mediaPlayerListeningTracker.attachToPlayer(currentPlayer, playlistItemProperty.get().getTrackRecord());
        playbackStatusProperty.set(PlaybackState.LAUNCHING);
        Optional<PlaylistItem> nextForPlayback = playlistModel.getNextForPlayback();

        // Preload next track
        if (nextForPlayback.isPresent()) {
            nextPlaylistItem = nextForPlayback.get();
            log.info("Preloading: {}", nextPlaylistItem.getTitle());
            Media nextMedia = mediaProvider.getMedia(nextPlaylistItem.getTrackRecord());
            nextPlayer = new MediaPlayer(nextMedia);
            nextPlayer.setOnError(() -> log.error("Error loading next track:", nextPlayer.getError()));
        }

        // Set up transition to next track
        currentPlayer.setOnEndOfMedia(() -> {
            playbackStatusProperty.set(PlaybackState.ENDED);
            eventPublisher.publishEvent(new PlayPauseEvent(this));
        });
        currentPlayer.setVolume(volumeProperty.getValue() / 100);
        volumeProperty.addListener(ov -> currentPlayer.setVolume(volumeProperty.getValue() / 100));
        currentPlayer.currentTimeProperty().addListener(ov ->
                playbackProgressListener.onProgressUpdated(
                        currentPlayer.getCurrentTime(),
                        Duration.seconds(playlistItemProperty.get().getDuration())
                ));
        currentPlayer.setAudioSpectrumThreshold(waveformDataListener.getAudioSpectrumThreshold());
        currentPlayer.setAudioSpectrumInterval(waveformDataListener.getAudioSpectrumInterval());
        currentPlayer.setAudioSpectrumListener(waveformDataListener);
        currentPlayer.play();
        playbackStatusProperty.set(PlaybackState.PLAYING);
    }

    public void seek(Duration duration) {
        currentPlayer.seek(duration);
    }

    private boolean isFirstSong() {
        return currentPlayer == null;
    }

    @EventListener
    public void onApplicationShutdown(UserShutdownApplication event) {
        stop();
    }

    public void stop() {
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.dispose();
            currentPlayer = null;
        }
        if (nextPlayer != null) {
            nextPlayer.dispose();
            nextPlayer = null;
        }
        playbackStatusProperty.set(PlaybackState.STOPPED);
    }
}
