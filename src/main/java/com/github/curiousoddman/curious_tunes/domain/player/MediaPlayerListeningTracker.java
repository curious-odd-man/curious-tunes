package com.github.curiousoddman.curious_tunes.domain.player;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.player.PlayedThirdOfTrackEvent;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaPlayerListeningTracker {
    private final ApplicationEventPublisher eventPublisher;
    private final WindowsVolumeService windowsVolumeService;

    private Duration totalListenedTime = Duration.ZERO;
    private Duration lastRecordedTime = Duration.ZERO;

    public void attachToPlayer(MediaPlayer mediaPlayer, TrackRecord trackRecord) {
        totalListenedTime = Duration.ZERO;
        lastRecordedTime = Duration.ZERO;

        ReadOnlyObjectProperty<Duration> durationReadOnlyObjectProperty = mediaPlayer.currentTimeProperty();

        // Track actual listening time
        ChangeListener<Duration> durationChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Duration> obs, Duration oldTime, Duration newTime) {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    Duration timeDiff = newTime.subtract(lastRecordedTime);

                    // Only count as listening if time moved forward naturally (< 1 second jump)
                    if (timeDiff.greaterThan(Duration.ZERO) && timeDiff.lessThan(Duration.seconds(1))) {
                        totalListenedTime = totalListenedTime.add(timeDiff);

                        // Check if 30% threshold reached
                        Duration totalDuration = mediaPlayer.getTotalDuration();
                        if (totalDuration != null && !totalDuration.isUnknown()) {
                            double listenedPercentage = totalListenedTime.toMillis() / totalDuration.toMillis();

                            if (listenedPercentage >= 0.30) {
                                log.info("Preparing to report 30% track played");
                                double mpVol = mediaPlayer.getVolume();
                                float master = windowsVolumeService.getMasterVolume();
                                double effective = windowsVolumeService.getEffectiveVolume(mpVol);

                                log.info(String.format("MediaPlayer volume: %.0f%%", mpVol * 100));
                                log.info(String.format("Windows master:     %.0f%%", master * 100));
                                double volumeToReport = effective * 100;
                                log.info(String.format("Effective volume:   %.0f%%", volumeToReport));

                                log.info("Played 30 percent, current volume is: {}", volumeToReport);
                                eventPublisher.publishEvent(new PlayedThirdOfTrackEvent(
                                        MediaPlayerListeningTracker.this,
                                        trackRecord,
                                        (int) volumeToReport));
                                durationReadOnlyObjectProperty.removeListener(this);
                            }
                        }
                    }

                    lastRecordedTime = newTime;
                }
            }
        };
        durationReadOnlyObjectProperty.addListener(durationChangeListener);
    }
}
