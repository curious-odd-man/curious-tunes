package com.github.curiousoddman.curious_tunes.ui.controller.custom;

import com.github.curiousoddman.curious_tunes.util.async.WaveformWriter;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.LongConsumer;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProgressCanvasController implements TrackProgressController {
    private static final int NUM_BARS = 2000;
    private static final double BAR_GAP = 0.0;
    private static final int AUDIO_THRESHOLD_DB = -80;

    private static final Color BG_COLOR = Color.web("#3a4149");
    private static final Color PLAYED_TOP = Color.web("#ffa500");
    private static final Color PLAYED_BOT = Color.web("#f0c040");
    private static final Color UNPLAYED_TOP = Color.web("#8fa5b2");
    private static final Color UNPLAYED_BOT = Color.web("#6d8898");

    private final WaveformWriter waveformWriter;
    private final MusicCurveAnimation musicCurveAnimation;

    // ── Waveform data ────────────────────────────────────────────────────────
    private final double[] waveform = new double[NUM_BARS];

    private Canvas canvas;
    private GraphicsContext graphicsContext;
    private WritableImage playedImage;
    private WritableImage unplayedImage;
    private Duration currentDuration;
    private Duration totalDuration;
    private AnimationTimer timer;

    private LongConsumer animation = now -> render();

    public void init(StackPane parentPane, Canvas canvas) {
        this.canvas = canvas;

        parentPane.widthProperty().addListener(w -> {
            canvas.setWidth(parentPane.getWidth());
        });

        graphicsContext = canvas.getGraphicsContext2D();
        clearWaveform(graphicsContext);
        parentPane.setStyle("-fx-background-color: #0d0d0d; -fx-background-radius: 8;");
        setProgressZero();
    }

    @SneakyThrows
    public void startProgress() {
        if (timer == null) {
            timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    animation.accept(now);
                }
            };
            timer.start();
        }

        setProgressZero();
        clearWaveform(graphicsContext);
        waveformWriter.stop();
        Arrays.fill(waveform, 0);
    }

    public void setProgressZero() {
        currentDuration = Duration.hours(0);
        totalDuration = Duration.hours(0);
    }

    public void setProgress(Duration currentDuration, Duration totalDuration) {
        this.currentDuration = currentDuration;
        this.totalDuration = totalDuration;
    }

    @SneakyThrows
    @Override
    public void spectrumDataUpdate(double timestamp,
                                   double duration,
                                   float[] magnitudes,
                                   float[] phases) {
        double progress = timestamp / totalDuration.toSeconds();
        int barIndex = Math.min(NUM_BARS - 1, (int) (progress * NUM_BARS));

        float loudestMagnitude = getLoudestMagnitude(magnitudes);

        double barMagnitude = waveform[barIndex];
        double currentMagnitude = Math.min(1.0, loudestMagnitude / -AUDIO_THRESHOLD_DB);
        if (barMagnitude == 0) {
            barMagnitude = Math.max(0, currentMagnitude);
        } else {
            barMagnitude = Math.max(barMagnitude, currentMagnitude);
        }

        waveformWriter.append(timestamp, duration, magnitudes, phases, loudestMagnitude, barMagnitude);
        waveform[barIndex] = barMagnitude;
        buildImages();
    }

    @Override
    public int getAudioSpectrumThreshold() {
        return AUDIO_THRESHOLD_DB;
    }

    @Override
    public double getAudioSpectrumInterval() {
        return 0.1;
    }

    @Override
    public void startLoadingAnimation() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        GraphicsContext graphicsContext2D = canvas.getGraphicsContext2D();
        animation = now -> {
            musicCurveAnimation.update(now, width, height);
            musicCurveAnimation.draw(graphicsContext2D, width, height);
        };
    }

    @Override
    public void stopLoadingAnimation() {
        animation = now -> render();
    }

    private void buildImages() {
        playedImage = renderImage(PLAYED_TOP, PLAYED_BOT);
        unplayedImage = renderImage(UNPLAYED_TOP, UNPLAYED_BOT);
    }

    private WritableImage renderImage(Color topColor, Color botColor) {
        double height = canvas.getHeight();
        double width = canvas.getWidth();
        WritableImage img = new WritableImage((int) width, (int) height);
        PixelWriter pw = img.getPixelWriter();
        double barW = width / NUM_BARS;
        double centerY = height / 2.0;

        // Background
        int bgArgb = toArgb(BG_COLOR);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pw.setArgb(x, y, bgArgb);
            }
        }

        // Bars
        for (int b = 0; b < NUM_BARS; b++) {
            double amp = Math.max(0.03, waveform[b]);
            double halfH = amp * (centerY - 2);
            int xStart = (int) (b * barW) + 1;
            int xEnd = (int) Math.min((b + 1) * barW - BAR_GAP, width - 1);
            int yTop = (int) (centerY - halfH);
            int yBot = (int) (centerY + halfH);

            for (int x = xStart; x <= xEnd; x++) {
                for (int y = yTop; y <= yBot && y < height; y++) {
                    double t = (yBot > yTop) ? (double) (y - yTop) / (yBot - yTop) : 0;
                    Color color = topColor.interpolate(botColor, t);
                    pw.setArgb(x, y, toArgb(color));
                }
            }
        }
        return img;
    }

    private static int toArgb(Color c) {
        return (0xFF000000)
                | ((int) (c.getRed() * 255) << 16)
                | ((int) (c.getGreen() * 255) << 8)
                | (int) (c.getBlue() * 255);
    }

    private void render() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        clearWaveform(graphicsContext);
        double currentProgress = currentDuration.toMillis() / totalDuration.toMillis();
        int splitX = (int) (currentProgress * width);

        if (splitX > 0) {
            graphicsContext.drawImage(playedImage,
                    0, 0, splitX, height,   // source rect
                    0, 0, splitX, height);  // dest rect
        }

        if (splitX < width) {
            graphicsContext.drawImage(unplayedImage,
                    splitX, 0, width - splitX, height,
                    splitX, 0, width - splitX, height);
        }
    }

    private void clearWaveform(GraphicsContext gc) {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, width, height);
    }

    private static float getLoudestMagnitude(float[] magnitudes) {
        float loudestMagnitude = 0;
        for (float magnitude : magnitudes) {
            float zeroBasedMagnitude = magnitude - AUDIO_THRESHOLD_DB;
            if (loudestMagnitude < zeroBasedMagnitude) {
                loudestMagnitude = zeroBasedMagnitude;
            }
        }
        return loudestMagnitude;
    }
}
