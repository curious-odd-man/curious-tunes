package com.github.curiousoddman.curious_tunes.ui.playback;

import com.github.curiousoddman.curious_tunes.model.WaveformModel;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import javafx.animation.AnimationTimer;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

import java.util.function.DoubleConsumer;

/**
 * Draws the shared {@link WaveformModel} for the playback window. Click to seek.
 * <p>
 * Redraws only when something changed (waveform data, playhead position, size or colors),
 * and only while started (the window is showing).
 */
public class PlaybackWaveformView {
    private static final double MIN_AMPLITUDE = 0.03;

    private final WaveformModel  model;
    private final StackPane      pane   = new StackPane();
    private final Canvas         canvas = new Canvas();
    private final AnimationTimer timer;

    private PlaybackPalette palette = PlaybackPalette.DEFAULT;
    private LinearGradient  playedPaint;
    private LinearGradient  unplayedPaint;

    private double currentMillis;
    private double totalMillis;

    private       double  hoverX      = -1;       // pointer position over the waveform, -1 when it is not over it
    private       int     drawnHoverX = -2;
    private final Tooltip hoverTip    = new Tooltip();

    private boolean dirty        = true;
    private int     drawnVersion = -1;
    private int     drawnSplitX  = -1;
    private double  drawnWidth   = -1;
    private double  drawnHeight  = -1;

    public PlaybackWaveformView(WaveformModel model, DoubleConsumer onSeek) {
        this.model = model;

        // A Canvas cannot shrink with its parent unless the parent's minimum size is overridden.
        pane.setMinSize(0, 0);
        pane.getChildren()
            .add(canvas);
        canvas.widthProperty()
              .bind(pane.widthProperty());
        canvas.heightProperty()
              .bind(pane.heightProperty());
        pane.setCursor(Cursor.HAND);
        pane.setOnMouseClicked(event -> {
            double width = pane.getWidth();
            if (width > 0 && totalMillis > 0) {
                double fraction = Math.max(0, Math.min(1, event.getX() / width));
                onSeek.accept(fraction);
            }
        });

        pane.setOnMouseMoved(event -> {
            hoverX = event.getX();
            showHoverTime(event.getScreenX(), event.getScreenY());
        });
        pane.setOnMouseExited(event -> {
            hoverX = -1;
            hoverTip.hide();
        });

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderIfNeeded();
            }
        };
    }

    /**
     * The time under the pointer. Where the waveform is not drawn yet (it fills in while the track plays,
     * so on the first listen the part that is ahead is empty) a hint says so.
     */
    private void showHoverTime(double screenX, double screenY) {
        double width = pane.getWidth();
        if (width <= 0 || totalMillis <= 0) {
            hoverTip.hide();
            return;
        }
        double   fraction = Math.max(0, Math.min(1, hoverX / width));
        String   text     = TimeUtils.secondsToHumanTime((int) (fraction * totalMillis / 1000));
        double[] bars     = model.getBars();
        int      index    = Math.min(bars.length - 1, (int) (fraction * bars.length));
        if (bars[index] == 0 && fraction * totalMillis > currentMillis) {
            text += "\nThe waveform fills in as the track plays";
        }
        hoverTip.setText(text);
        if (hoverTip.isShowing()) {
            hoverTip.setAnchorX(screenX + 12);
            hoverTip.setAnchorY(screenY + 16);
        } else {
            hoverTip.show(pane, screenX + 12, screenY + 16);
        }
    }

    public StackPane getNode() {
        return pane;
    }

    public void start() {
        dirty = true;
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void setProgress(Duration current, Duration total) {
        currentMillis = current.toMillis();
        totalMillis = total.toMillis();
    }

    public void setPalette(PlaybackPalette palette) {
        this.palette = palette;
        playedPaint = null;
        unplayedPaint = null;
        dirty = true;
    }

    private void renderIfNeeded() {
        double width  = canvas.getWidth();
        double height = canvas.getHeight();
        if (width < 2 || height < 2) {
            return;
        }

        int splitX = totalMillis > 0
                ? (int) Math.max(0, Math.min(width, currentMillis / totalMillis * width))
                : 0;
        int version = model.getVersion();

        int hover = (int) hoverX;
        if (!dirty
                && hover == drawnHoverX
                && version == drawnVersion
                && splitX == drawnSplitX
                && width == drawnWidth
                && height == drawnHeight) {
            return;
        }
        render(width, height, splitX);
        dirty = false;
        drawnVersion = version;
        drawnHoverX = hover;
        drawnSplitX = splitX;
        drawnWidth = width;
        drawnHeight = height;
    }

    private void render(double width, double height, int splitX) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.color(0, 0, 0, 0.28));
        gc.fillRoundRect(0, 0, width, height, 10, 10);

        if (playedPaint == null || unplayedPaint == null) {
            playedPaint = gradient(palette.accent(), palette.accent()
                                                            .deriveColor(14, 0.9, 1, 1), height);
            unplayedPaint = gradient(palette.unplayed(), palette.unplayed()
                                                                .deriveColor(0, 1, 0.8, 1), height);
        }

        double[] bars     = model.getBars();
        int      barCount = bars.length;
        double   centerY  = height / 2;
        int      columns  = (int) width;

        for (int x = 0; x < columns; x++) {
            int    from      = (int) (x * (double) barCount / width);
            int    to        = Math.max(from + 1, (int) ((x + 1) * (double) barCount / width));
            double amplitude = MIN_AMPLITUDE;
            for (int i = from; i < to && i < barCount; i++) {
                amplitude = Math.max(amplitude, bars[i]);
            }
            double half = amplitude * (centerY - 3);
            gc.setFill(x < splitX ? playedPaint : unplayedPaint);
            gc.fillRect(x, centerY - half, 1, half * 2);
        }

        // Playhead and, while the pointer is over the waveform, where a click would seek to.
        if (totalMillis > 0) {
            gc.setFill(Color.color(1, 1, 1, 0.9));
            gc.fillRect(Math.max(0, splitX - 1), 2, 2, height - 4);
        }
        if (hoverX >= 0) {
            gc.setFill(Color.color(1, 1, 1, 0.45));
            gc.fillRect(Math.floor(hoverX), 2, 1, height - 4);
        }
    }

    private static LinearGradient gradient(Color top, Color bottom, double height) {
        return new LinearGradient(0, 0, 0, height, false, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom));
    }
}
