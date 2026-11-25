package com.github.curiousoddman.curious_tunes.ui.playback.calibration;

import com.github.curiousoddman.curious_tunes.domain.player.AudioPlayer;
import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
import com.github.curiousoddman.curious_tunes.event.PlayPauseEvent;
import com.github.curiousoddman.curious_tunes.model.PlaybackState;
import com.github.curiousoddman.curious_tunes.model.SpectrumModel;
import com.github.curiousoddman.curious_tunes.ui.controller.custom.ProgressCanvasController;
import com.github.curiousoddman.curious_tunes.ui.playback.PlaybackPalette;
import com.github.curiousoddman.curious_tunes.ui.playback.PlaybackSpectrumView;
import com.github.curiousoddman.curious_tunes.ui.playback.VisualiserSync;
import com.github.curiousoddman.curious_tunes.util.Accessibility;
import com.github.curiousoddman.curious_tunes.util.UiStyles;
import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The visualiser sync calibration (doc/visualiser-calibration.md, section 2): plays a click track through a JavaFX
 * media player, like a song, and shows the visualiser and a large flash driven by its spectrum. The user moves the
 * slider until the flash lands on the clicks; the value is saved as the visualiser delay.
 * <p>
 * Built in code, like the playback window. Everything here runs on the JavaFX application thread.
 */
@Slf4j
@Lazy
@Component
@RequiredArgsConstructor
public class VisualiserCalibrationWindow {
    private static final int    STEP_MILLIS        = 5;
    private static final int    FINE_STEP_MILLIS   = 10;
    /**
     * Share of the full dB range above which a delayed spectrum counts as a click. Low, because the track is
     * silent between clicks and the player's volume may lower the measured level.
     */
    private static final double CLICK_LEVEL        = 0.3;
    private static final double FLASH_FADE_PER_SEC = 4;
    /**
     * A click this close to the loudest one heard (in loudness units; 0.035 is about 3 dB) is an accented one.
     */
    private static final double ACCENT_MARGIN      = 0.035;
    private static final double DIM_FLASH          = 0.55;
    private static final double FLASH_SIZE         = 150;
    private static final Color  PANEL_COLOR        = Color.web("#1e2329");

    private final AudioPlayer               audioPlayer;
    private final VisualiserSync            visualiserSync;
    private final ApplicationEventPublisher eventPublisher;

    private Stage stage;

    /**
     * Opens the window over {@code owner}, or brings it to the front if it is open already.
     */
    public void show(Window owner) {
        if (stage != null) {
            stage.toFront();
            return;
        }
        new Session(owner).open();
    }

    /**
     * One opening of the window: its player, temporary file and timers, all released when it closes.
     */
    private final class Session {
        private final Window                owner;
        private final SpectrumModel         spectrum     = new SpectrumModel();
        private final SimpleIntegerProperty delay        = new SimpleIntegerProperty(visualiserSync.getDelayMillis());
        private final PlaybackSpectrumView  spectrumView = new PlaybackSpectrumView(spectrum, delay);
        private final Region                flash        = new Region();
        private final Label                 errorLabel   = new Label();

        private Path                clickFile;
        private MediaPlayer         player;
        private AnimationTimer      flashTimer;
        private boolean             pausedMusic;
        private SpectrumModel.Frame flashedFrame;
        private double              flashLevel;
        private double              loudestClick;
        private long                lastFlashNanos;

        Session(Window owner) {
            this.owner = owner;
        }

        void open() {
            stage = new Stage();
            stage.setTitle("Calibrate visualiser");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) {
                stage.initOwner(owner);
            }
            Scene scene = new Scene(buildContent());
            UiStyles.apply(scene);
            Accessibility.nameIconControls(scene.getRoot());
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setOnHidden(e -> close());

            pauseMusic();
            startClicks();
            spectrumView.start();
            startFlash();
            stage.show();
        }

        private VBox buildContent() {
            Label title = new Label("Visualiser sync");
            title.setFont(Font.font(null, FontWeight.BOLD, 18));

            Label hint = new Label("Listen to the clicks and watch the flash. Move the slider until the flash lands "
                    + "exactly on each click. If it's hard to tell, choose a little higher: a picture slightly late "
                    + "is less noticeable than early.");
            hint.setWrapText(true);
            Label devices = new Label("Run this again after changing headphones or speakers.");
            devices.setWrapText(true);
            devices.setOpacity(0.75);

            flash.setPrefSize(FLASH_SIZE, FLASH_SIZE);
            flash.setMinSize(FLASH_SIZE, FLASH_SIZE);
            flash.setMaxSize(FLASH_SIZE, FLASH_SIZE);
            flash.setBackground(new Background(new BackgroundFill(PlaybackPalette.DEFAULT.accent(), new CornerRadii(16), Insets.EMPTY)));
            flash.setOpacity(0);
            flash.setAccessibleRole(AccessibleRole.IMAGE_VIEW);
            flash.setAccessibleText("Flash on each click");
            StackPane flashSlot = new StackPane(flash);
            flashSlot.setBackground(new Background(new BackgroundFill(Color.web("#2a3038"), new CornerRadii(16), Insets.EMPTY)));
            flashSlot.setPrefSize(FLASH_SIZE, FLASH_SIZE);
            flashSlot.setMinSize(FLASH_SIZE, FLASH_SIZE);

            Region bars = spectrumView.getNode();
            bars.setPrefSize(420, FLASH_SIZE);
            bars.setMinSize(420, FLASH_SIZE);
            HBox visuals = new HBox(24, flashSlot, bars);
            visuals.setAlignment(Pos.CENTER_LEFT);
            visuals.setPadding(new Insets(20));
            visuals.setBackground(new Background(new BackgroundFill(PANEL_COLOR, new CornerRadii(12), Insets.EMPTY)));

            Slider slider = new Slider(0, UserPreferencesService.MAX_VISUALISER_DELAY_MS, delay.get());
            slider.setMajorTickUnit(100);
            slider.setMinorTickCount(3);
            slider.setShowTickMarks(true);
            slider.setShowTickLabels(true);
            slider.setBlockIncrement(STEP_MILLIS);
            slider.setAccessibleText("Visualiser delay in milliseconds");
            HBox.setHgrow(slider, Priority.ALWAYS);
            slider.valueProperty()
                  .addListener((obs, was, value) -> setDelay((int) (Math.round(value.doubleValue() / STEP_MILLIS) * STEP_MILLIS)));
            delay.addListener((obs, was, value) -> {
                if (Math.round(slider.getValue()) != value.intValue()) {
                    slider.setValue(value.intValue());
                }
            });

            Label valueLabel = new Label();
            valueLabel.setMinWidth(64);
            valueLabel.setFont(Font.font(null, FontWeight.BOLD, 14));
            valueLabel.textProperty()
                      .bind(delay.asString("%d ms"));

            Button minus = new Button("−" + FINE_STEP_MILLIS + " ms");
            minus.setOnAction(e -> setDelay(delay.get() - FINE_STEP_MILLIS));
            minus.setAccessibleText("Decrease the delay by " + FINE_STEP_MILLIS + " milliseconds");
            Button plus = new Button("+" + FINE_STEP_MILLIS + " ms");
            plus.setOnAction(e -> setDelay(delay.get() + FINE_STEP_MILLIS));
            plus.setAccessibleText("Increase the delay by " + FINE_STEP_MILLIS + " milliseconds");

            Label delayLabel = new Label("Delay");
            HBox  sliderRow  = new HBox(10, delayLabel, minus, slider, plus, valueLabel);
            sliderRow.setAlignment(Pos.CENTER_LEFT);

            errorLabel.setWrapText(true);
            errorLabel.setTextFill(Color.web("#d9534f"));
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            Button reset = new Button("Reset");
            reset.setOnAction(e -> setDelay(UserPreferencesService.DEFAULT_VISUALISER_DELAY_MS));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button cancel = new Button("Cancel");
            cancel.setCancelButton(true);
            cancel.setOnAction(e -> stage.hide());
            Button save = new Button("Save");
            save.setDefaultButton(true);
            save.setOnAction(e -> {
                visualiserSync.save(delay.get());
                log.info("Visualiser delay saved: {} ms", delay.get());
                stage.hide();
            });
            HBox buttons = new HBox(10, reset, spacer, cancel, save);

            VBox root = new VBox(14, title, hint, devices, visuals, sliderRow, errorLabel, buttons);
            root.setPadding(new Insets(24));
            root.setPrefWidth(680);
            return root;
        }

        private void setDelay(int millis) {
            delay.set(UserPreferencesService.clampVisualiserDelay(millis));
        }

        private void pauseMusic() {
            if (audioPlayer.getPlaybackStatusProperty()
                           .get() == PlaybackState.PLAYING) {
                eventPublisher.publishEvent(new PlayPauseEvent(this));
                pausedMusic = true;
            }
        }

        private void resumeMusic() {
            if (pausedMusic && audioPlayer.getPlaybackStatusProperty()
                                          .get() == PlaybackState.PAUSED) {
                eventPublisher.publishEvent(new PlayPauseEvent(this));
            }
            pausedMusic = false;
        }

        private void startClicks() {
            try {
                clickFile = Files.createTempFile("ctunes-click-track", ".wav");
                clickFile.toFile()
                         .deleteOnExit();
                ClickTrack.write(clickFile);

                player = new MediaPlayer(new Media(clickFile.toUri()
                                                            .toString()));
                player.setAudioSpectrumThreshold(ProgressCanvasController.AUDIO_THRESHOLD_DB);
                player.setAudioSpectrumInterval(ProgressCanvasController.SPECTRUM_INTERVAL_SEC);
                player.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) ->
                        spectrum.update(magnitudes, ProgressCanvasController.AUDIO_THRESHOLD_DB));
                player.setVolume(audioPlayer.getVolumeProperty()
                                            .get() / 100);
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setOnError(() -> showError(player.getError()));
                player.play();
            } catch (Exception e) {
                showError(e);
            }
        }

        private void showError(Throwable error) {
            log.warn("Visualiser calibration: the click track cannot play", error);
            errorLabel.setText("The click track cannot play"
                    + (error == null || error.getMessage() == null ? "." : ": " + error.getMessage())
                    + " Check that a sound output is connected.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            if (stage != null) {
                stage.sizeToScene();
            }
        }

        /**
         * Lights the flash when a click reaches the delayed spectrum, then fades it out. It is the onset that
         * the user lines up with the sound, so the flash comes on at full strength in one frame.
         */
        private void startFlash() {
            flashTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    double seconds = lastFlashNanos == 0 ? 0 : (now - lastFlashNanos) / 1e9;
                    lastFlashNanos = now;
                    flashLevel = Math.max(0, flashLevel - FLASH_FADE_PER_SEC * seconds);

                    SpectrumModel.Frame frame = spectrum.frameAt(System.nanoTime() - delay.get() * 1_000_000L);
                    if (frame != null && frame != flashedFrame) {
                        flashedFrame = frame;
                        double level = SpectrumModel.loudness(frame.magnitudes(), spectrum.getThresholdDb());
                        if (level >= CLICK_LEVEL) {
                            // The louder fourth click (about 5 dB up) gives a full flash, the others a dimmer one.
                            loudestClick = Math.max(loudestClick, level);
                            boolean accent = level >= loudestClick - ACCENT_MARGIN;
                            flashLevel = Math.max(flashLevel, accent ? 1 : DIM_FLASH);
                        }
                    }
                    flash.setOpacity(flashLevel);
                }
            };
            flashTimer.start();
        }

        private void close() {
            if (flashTimer != null) {
                flashTimer.stop();
            }
            spectrumView.stop();
            if (player != null) {
                player.setAudioSpectrumListener(null);
                player.stop();
                player.dispose();
                player = null;
            }
            if (clickFile != null) {
                try {
                    Files.deleteIfExists(clickFile);
                } catch (Exception e) {
                    log.debug("Cannot delete {} now; it is deleted on exit", clickFile, e);
                }
            }
            resumeMusic();
            stage = null;
        }
    }
}
