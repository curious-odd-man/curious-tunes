package com.github.curiousoddman.curious_tunes.ui.playback;

import com.github.curiousoddman.curious_tunes.model.SpectrumModel;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ObservableIntegerValue;
import javafx.scene.AccessibleRole;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;

/**
 * Spectrum visualiser of the playback window, shown under the album art in the maximized view:
 * bars from bass (left) to treble (right) with peak caps, and a short fading reflection below them.
 * Colors follow the album art palette.
 * <p>
 * Draws on every frame while started and something moves; stops drawing once everything has fallen to zero
 * (paused or stopped), and starts again with the next spectrum.
 */
public class PlaybackSpectrumView {
    public static final int BAR_COUNT = 48;

    /**
     * Share of the height used by the bars; the rest is the reflection.
     */
    private static final double BARS_SHARE      = 0.76;
    private static final double GAP             = 3;
    private static final double CAP_HEIGHT      = 3;
    private static final double MIN_BAR_HEIGHT  = 2;
    /**
     * No spectrum for this long means nothing is playing (the player reports every 40 ms while playing).
     */
    private static final long   SILENCE_NANOS   = 250_000_000L;

    private final SpectrumModel          model;
    private final ObservableIntegerValue delayMillis;
    private final SpectrumBars   bars   = new SpectrumBars(BAR_COUNT);
    private final StackPane      pane   = new StackPane();
    private final Canvas         canvas = new Canvas();
    private final AnimationTimer timer;

    private PlaybackPalette palette = PlaybackPalette.DEFAULT;
    private Paint           barPaint;
    private Paint           reflectionPaint;
    private Color           capColor;

    private long                lastFrameNanos;
    private SpectrumModel.Frame shownFrame;
    private boolean             dirty = true;

    /**
     * @param model       where the spectrum comes from
     * @param delayMillis how long after a spectrum arrives it is drawn: the time the sound takes from the decoder
     *                    (where the spectrum is measured) to the speakers, so the bars move with what is heard
     *                    (see {@link VisualiserSync}); read on every frame, so a change shows at once
     */
    public PlaybackSpectrumView(SpectrumModel model, ObservableIntegerValue delayMillis) {
        this.model = model;
        this.delayMillis = delayMillis;

        // A Canvas cannot shrink with its parent unless the parent's minimum size is overridden.
        pane.setMinSize(0, 0);
        pane.getChildren()
            .add(canvas);
        canvas.widthProperty()
              .bind(pane.widthProperty());
        canvas.heightProperty()
              .bind(pane.heightProperty());
        canvas.widthProperty()
              .addListener(o -> dirty = true);
        canvas.heightProperty()
              .addListener(o -> {
                  barPaint = null;          // the gradients depend on the height
                  dirty = true;
              });
        pane.setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        pane.setAccessibleText("Audio spectrum");

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                frame(now);
            }
        };
    }

    public StackPane getNode() {
        return pane;
    }

    public void start() {
        lastFrameNanos = 0;
        dirty = true;
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void setPalette(PlaybackPalette palette) {
        this.palette = palette;
        barPaint = null;
        dirty = true;
    }

    private void frame(long now) {
        double seconds = lastFrameNanos == 0 ? 0 : (now - lastFrameNanos) / 1e9;
        lastFrameNanos = now;

        long                heardNanos = System.nanoTime() - delayMillis.get() * 1_000_000L;
        SpectrumModel.Frame frame      = model.frameAt(heardNanos);
        if (frame == null || heardNanos - frame.arrivedNanos() > SILENCE_NANOS) {
            bars.silence();
        } else if (frame != shownFrame) {
            shownFrame = frame;
            bars.setSpectrum(frame.magnitudes(), model.getThresholdDb());
        }

        boolean idle = bars.isIdle();
        if (idle && !dirty) {
            return;
        }
        bars.step(seconds);
        render();
        dirty = false;
    }

    private void render() {
        double width  = canvas.getWidth();
        double height = canvas.getHeight();
        if (width < 2 || height < 2) {
            return;
        }
        double barsHeight = Math.floor(height * BARS_SHARE);
        double reflHeight = height - barsHeight - 2;
        if (barPaint == null) {
            Color accent = palette.accent();
            barPaint = new LinearGradient(0, barsHeight, 0, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0, accent),
                    new Stop(1, accent.deriveColor(0, 1, 1, 0.6)));
            reflectionPaint = new LinearGradient(0, barsHeight + 2, 0, height, false, CycleMethod.NO_CYCLE,
                    new Stop(0, accent.deriveColor(0, 1, 1, 0.35)),
                    new Stop(1, accent.deriveColor(0, 1, 1, 0)));
            capColor = palette.text()
                              .deriveColor(0, 1, 1, 0.85);
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        double[] levels   = bars.getLevels();
        double[] peaks    = bars.getPeaks();
        double   barWidth = Math.max(1, (width - GAP * (BAR_COUNT - 1)) / BAR_COUNT);
        double   step     = barWidth + GAP;
        double   capRoom  = CAP_HEIGHT + 2;      // keep the cap inside the area at full height

        for (int i = 0; i < BAR_COUNT; i++) {
            double x   = Math.round(i * step);
            double w   = Math.max(1, Math.round((i + 1) * step - GAP) - x);
            double bar = Math.max(MIN_BAR_HEIGHT, levels[i] * (barsHeight - capRoom));

            gc.setFill(barPaint);
            gc.fillRoundRect(x, barsHeight - bar, w, bar, 3, 3);

            double refl = Math.min(reflHeight, bar * reflHeight / (barsHeight - capRoom));
            if (refl > 0.5) {
                gc.setFill(reflectionPaint);
                gc.fillRect(x, barsHeight + 2, w, refl);
            }

            if (peaks[i] > 0.01) {
                double capY = barsHeight - peaks[i] * (barsHeight - capRoom) - CAP_HEIGHT - 2;
                gc.setFill(capColor);
                gc.fillRoundRect(x, Math.max(0, capY), w, CAP_HEIGHT, 2, 2);
            }
        }
    }
}
