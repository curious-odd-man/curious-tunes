package com.github.curiousoddman.curious_tunes.ui.controller.custom;

import com.github.curiousoddman.curious_tunes.model.SpectrumModel;
import com.github.curiousoddman.curious_tunes.model.WaveformModel;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import com.github.curiousoddman.curious_tunes.util.async.WaveformWriter;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.LongConsumer;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProgressCanvasController implements TrackProgressController {
    private static final int    NUM_BARS           = WaveformModel.NUM_BARS;
    public static final  int    AUDIO_THRESHOLD_DB = -80;
    /**
     * How often the player reports the spectrum. Fast enough for the playback window's visualiser to look smooth.
     */
    public static final  double SPECTRUM_INTERVAL_SEC = 0.04;
    /**
     * The waveform images are rebuilt no more often than this while the waveform fills in during playback.
     */
    private static final long   IMAGE_REBUILD_NANOS   = 100_000_000L;

    // Defaults; the real colors come from the -ct-seek-* tokens of global.css (see loadColorsFromCss).
    private Color bgColor     = Color.web("#3a4149");
    private Color playedTop   = Color.web("#ffa500");
    private Color playedBot   = Color.web("#f0c040");
    private Color unplayedTop = Color.web("#8fa5b2");
    private Color unplayedBot = Color.web("#6d8898");

    private final WaveformWriter      waveformWriter;
    private final MusicCurveAnimation musicCurveAnimation;
    private final WaveformModel       waveformModel;
    private final SpectrumModel       spectrumModel;

    private       Canvas          canvas;
    private       GraphicsContext graphicsContext;
    private       WritableImage   playedImage;
    private       WritableImage   unplayedImage;
    private       Duration        currentDuration;
    private       Duration        totalDuration;
    private       AnimationTimer  timer;
    private       double          hoverX   = -1;        // pointer over the seek bar, -1 when it is not over it
    private final Tooltip         hoverTip = new Tooltip();

    private LongConsumer animation   = this::renderIfChanged;
    private long         lastImageBuildNanos;
    private int[]        pixels      = new int[0];
    private boolean      imagesDirty = true;     // the waveform, the size or the colors changed: rebuild the images
    private boolean      frameDirty  = true;     // the canvas must be redrawn even if the playhead didn't move
    private int          drawnSplitX = -1;
    private double       drawnHoverX = -2;
    private boolean      wasOnScreen;

    public void init(StackPane parentPane, Canvas canvas) {
        this.canvas = canvas;

        parentPane.widthProperty()
                  .addListener(w -> {
                      canvas.setWidth(parentPane.getWidth());
                      imagesDirty = true;
                  });

        graphicsContext = canvas.getGraphicsContext2D();
        clearWaveform(graphicsContext);
        parentPane.getStyleClass()
                  .add("waveform-pane");
        parentPane.setCursor(Cursor.HAND);
        parentPane.setOnMouseMoved(event -> {
            hoverX = event.getX();
            showHoverTime(event.getScreenX(), event.getScreenY());
        });
        parentPane.setOnMouseExited(event -> {
            hoverX = -1;
            hoverTip.hide();
        });
        parentPane.sceneProperty()
                  .addListener((obs, oldScene, newScene) -> {
                      if (newScene != null) {
                          Platform.runLater(() -> loadColorsFromCss(parentPane));
                      }
                  });
        setProgressZero();
    }

    /**
     * Canvas drawing cannot use CSS directly, so the colors are read from small invisible nodes
     * that are styled by the same stylesheet as everything else.
     */
    private void loadColorsFromCss(StackPane parentPane) {
        Region bg           = colorProbe("seek-probe-bg");
        Region playedTopP   = colorProbe("seek-probe-played-top");
        Region playedBotP   = colorProbe("seek-probe-played-bottom");
        Region unplayedTopP = colorProbe("seek-probe-unplayed-top");
        Region unplayedBotP = colorProbe("seek-probe-unplayed-bottom");
        parentPane.getChildren()
                  .addAll(bg, playedTopP, playedBotP, unplayedTopP, unplayedBotP);
        try {
            bgColor = readColor(bg, bgColor);
            playedTop = readColor(playedTopP, playedTop);
            playedBot = readColor(playedBotP, playedBot);
            unplayedTop = readColor(unplayedTopP, unplayedTop);
            unplayedBot = readColor(unplayedBotP, unplayedBot);
            imagesDirty = true;
        } finally {
            parentPane.getChildren()
                      .removeAll(bg, playedTopP, playedBotP, unplayedTopP, unplayedBotP);
        }
    }

    private static Region colorProbe(String styleClass) {
        Region probe = new Region();
        probe.getStyleClass()
             .add(styleClass);
        probe.setManaged(false);
        probe.setMouseTransparent(true);
        probe.setPrefSize(0, 0);
        return probe;
    }

    private static Color readColor(Region probe, Color fallback) {
        probe.applyCss();
        Background background = probe.getBackground();
        if (background != null && !background.getFills()
                                             .isEmpty()
                && background.getFills()
                             .getFirst()
                             .getFill() instanceof Color color) {
            return color;
        }
        return fallback;
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
        waveformModel.reset();
        imagesDirty = true;
        frameDirty = true;
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
        double[] waveform = waveformModel.getBars();
        double   progress = timestamp / totalDuration.toSeconds();
        int      barIndex = Math.min(NUM_BARS - 1, (int) (progress * NUM_BARS));

        float loudestMagnitude = getLoudestMagnitude(magnitudes);

        double barMagnitude     = waveform[barIndex];
        double currentMagnitude = Math.min(1.0, loudestMagnitude / -AUDIO_THRESHOLD_DB);
        if (barMagnitude == 0) {
            barMagnitude = Math.max(0, currentMagnitude);
        } else {
            barMagnitude = Math.max(barMagnitude, currentMagnitude);
        }

        waveformWriter.append(timestamp, duration, magnitudes, phases, loudestMagnitude, barMagnitude);
        waveformModel.set(barIndex, barMagnitude);
        spectrumModel.update(magnitudes, AUDIO_THRESHOLD_DB);

        imagesDirty = true;          // rebuilt by the next frame, at most every IMAGE_REBUILD_NANOS
    }

    @Override
    public int getAudioSpectrumThreshold() {
        return AUDIO_THRESHOLD_DB;
    }

    @Override
    public double getAudioSpectrumInterval() {
        return SPECTRUM_INTERVAL_SEC;
    }

    @Override
    public void startLoadingAnimation() {
        double          width             = canvas.getWidth();
        double          height            = canvas.getHeight();
        GraphicsContext graphicsContext2D = canvas.getGraphicsContext2D();
        animation = now -> {
            musicCurveAnimation.update(now, width, height);
            musicCurveAnimation.draw(graphicsContext2D, width, height);
        };
    }

    @Override
    public void stopLoadingAnimation() {
        animation = this::renderIfChanged;
        frameDirty = true;
    }

    /**
     * Called on every frame, but only draws when the picture changed: the playhead moved by a pixel, the pointer
     * moved, the waveform grew, or the size or colors changed. Draws nothing while the window is hidden or
     * minimised (doc/performance-review.md, item 4.2).
     */
    private void renderIfChanged(long now) {
        boolean onScreen = isOnScreen();
        if (!onScreen) {
            wasOnScreen = false;
            return;
        }
        if (!wasOnScreen) {
            wasOnScreen = true;
            frameDirty = true;           // shown again: the canvas may not have the latest picture
        }
        if (imagesDirty && (playedImage == null || now - lastImageBuildNanos >= IMAGE_REBUILD_NANOS)) {
            lastImageBuildNanos = now;
            imagesDirty = false;
            buildImages();
            frameDirty = true;
        }
        int splitX = splitX();
        if (!frameDirty && splitX == drawnSplitX && hoverX == drawnHoverX) {
            return;
        }
        render();
        drawnSplitX = splitX;
        drawnHoverX = hoverX;
        frameDirty = false;
    }

    private boolean isOnScreen() {
        if (canvas.getScene() == null) {
            return false;
        }
        Window window = canvas.getScene()
                              .getWindow();
        return window != null && window.isShowing() && !(window instanceof Stage stage && stage.isIconified());
    }

    private int splitX() {
        if (totalDuration == null || currentDuration == null || totalDuration.toMillis() <= 0) {
            return 0;
        }
        return (int) (currentDuration.toMillis() / totalDuration.toMillis() * canvas.getWidth());
    }

    private void buildImages() {
        int width  = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();
        if (width <= 0 || height <= 0) {
            playedImage = null;
            unplayedImage = null;
            return;
        }
        if (pixels.length < width * height) {
            pixels = new int[width * height];
        }
        playedImage = renderImage(playedImage, width, height, playedTop, playedBot);
        unplayedImage = renderImage(unplayedImage, width, height, unplayedTop, unplayedBot);
    }

    /**
     * Draws the waveform into {@code reuse} when it has the right size, else into a new image; computed in one
     * array and written in one call (item 4.1).
     */
    private WritableImage renderImage(WritableImage reuse, int width, int height, Color topColor, Color botColor) {
        WritableImage img = reuse != null && (int) reuse.getWidth() == width && (int) reuse.getHeight() == height
                ? reuse
                : new WritableImage(width, height);
        WaveformRaster.render(pixels, width, height, waveformModel.getBars(),
                toArgb(bgColor), toArgb(topColor), toArgb(botColor));
        img.getPixelWriter()
           .setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return img;
    }

    private static int toArgb(Color c) {
        return (0xFF000000)
                | ((int) (c.getRed() * 255) << 16)
                | ((int) (c.getGreen() * 255) << 8)
                | (int) (c.getBlue() * 255);
    }

    private void showHoverTime(double screenX, double screenY) {
        double width = canvas.getWidth();
        if (width <= 0 || totalDuration == null || totalDuration.toMillis() <= 0) {
            hoverTip.hide();
            return;
        }
        double   fraction = Math.clamp(hoverX / width, 0, 1);
        String   text     = TimeUtils.secondsToHumanTime((int) (fraction * totalDuration.toSeconds()));
        double[] bars     = waveformModel.getBars();
        int      index    = Math.min(bars.length - 1, (int) (fraction * bars.length));
        if (bars[index] == 0 && currentDuration != null && fraction * totalDuration.toMillis() > currentDuration.toMillis()) {
            text += "\nThe waveform fills in as the track plays";
        }
        hoverTip.setText(text);
        if (hoverTip.isShowing()) {
            hoverTip.setAnchorX(screenX + 12);
            hoverTip.setAnchorY(screenY + 16);
        } else {
            hoverTip.show(canvas, screenX + 12, screenY + 16);
        }
    }

    private void render() {
        double width  = canvas.getWidth();
        double height = canvas.getHeight();
        clearWaveform(graphicsContext);
        int splitX = splitX();

        if (playedImage == null || unplayedImage == null) {
            return;
        }
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

        // Playhead, and where a click would seek to while the pointer is over the bar.
        if (totalDuration != null && totalDuration.toMillis() > 0) {
            graphicsContext.setFill(Color.color(1, 1, 1, 0.9));
            graphicsContext.fillRect(Math.max(0, splitX - 1), 2, 2, height - 4);
        }
        if (hoverX >= 0) {
            graphicsContext.setFill(Color.color(1, 1, 1, 0.45));
            graphicsContext.fillRect(Math.floor(hoverX), 2, 1, height - 4);
        }
    }

    private void clearWaveform(GraphicsContext gc) {
        double width  = canvas.getWidth();
        double height = canvas.getHeight();
        gc.setFill(bgColor);
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
