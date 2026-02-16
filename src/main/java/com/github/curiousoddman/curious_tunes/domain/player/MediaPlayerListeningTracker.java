package com.github.curiousoddman.curious_tunes.domain.player;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.player.PlayedThirdOfTrackEvent;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MediaPlayerListeningTracker {
    private final ApplicationEventPublisher eventPublisher;

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
                                eventPublisher.publishEvent(new PlayedThirdOfTrackEvent(MediaPlayerListeningTracker.this, trackRecord));
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
