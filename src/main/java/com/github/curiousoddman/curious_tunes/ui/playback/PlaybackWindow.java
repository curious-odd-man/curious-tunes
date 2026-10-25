package com.github.curiousoddman.curious_tunes.ui.playback;

import com.github.curiousoddman.curious_tunes.domain.player.AudioPlayer;
import com.github.curiousoddman.curious_tunes.domain.player.PlaybackSessionTimer;
import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
import com.github.curiousoddman.curious_tunes.event.PlayPauseEvent;
import com.github.curiousoddman.curious_tunes.event.SkipNextEvent;
import com.github.curiousoddman.curious_tunes.model.PlaybackState;
import com.github.curiousoddman.curious_tunes.model.WaveformModel;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryLyricsTabController;
import com.github.curiousoddman.curious_tunes.util.TimeUtils;
import com.github.curiousoddman.curious_tunes.util.async.DelayedAction;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The "now playing" window. It replaces the library window while open (the library window is hidden, not closed)
 * and has two views: simple (compact) and maximized (the window in its maximized state).
 * <p>
 * The UI is built in code rather than FXML because both views show the same controls in different arrangements.
 * Everything here runs on the JavaFX application thread.
 */
@Slf4j
@Lazy
@Component
@RequiredArgsConstructor
public class PlaybackWindow {
    private static final double MIN_WIDTH = 700;
    private static final double MIN_HEIGHT = 320;

    private static final double MIN_LYRICS_SIZE = 8;
    private static final double MAX_LYRICS_SIZE = 48;
    private static final double DEFAULT_LYRICS_SIZE = 16;

    private static final double SIMPLE_ART_SIZE = 96;
    private static final double SIMPLE_WAVEFORM_HEIGHT = 56;
    private static final double MAXIMIZED_WAVEFORM_HEIGHT = 100;
    private static final double LEFT_COLUMN_SHARE = 0.38;

    private static final String DIM = "pw-dim";
    private static final String NO_IMAGE = "/img/noimage.png";
    private static final String STYLESHEET = "/styles/playback-window.css";

    private static Image placeholderImage;

    private final AudioPlayer audioPlayer;
    private final PlaybackSessionTimer sessionTimer;
    private final WaveformModel waveformModel;
    private final UserPreferencesService userPreferencesService;
    private final ApplicationEventPublisher eventPublisher;

    private final DelayedAction delayedSaveBounds = new DelayedAction(500, TimeUnit.MILLISECONDS);
    private final List<Pane> layoutPanes = new ArrayList<>();

    // ── Window ───────────────────────────────────────────────────────────────
    private Stage stage;
    private Stage libraryStage;
    private BorderPane root;
    private VBox leftPane;
    private boolean maximized;

    // ── Current track ────────────────────────────────────────────────────────
    private ImageView artView;
    private Label titleLabel;
    private Label artistLabel;
    private Label albumLabel;
    private Label dotLabel;

    // ── Progress and controls ────────────────────────────────────────────────
    private PlaybackWaveformView waveformView;
    private Label elapsedLabel;
    private Label remainingLabel;
    private Label sessionLabel;
    private Button playPauseButton;
    private Button nextButton;
    private Button viewToggleButton;
    private Label volumeIcon;
    private Slider volumeSlider;
    private boolean volumeBound;
    private Timeline sessionTicker;

    // ── Up next ──────────────────────────────────────────────────────────────
    private HBox upNextCard;
    private ImageView cardArtView;
    private Label cardTitle;
    private Label cardArtist;
    private Label cardAlbum;
    private HBox upNextLine;
    private ImageView lineThumb;
    private Label lineHeader;
    private Label lineTitle;
    private Label lineSeparator;
    private Label lineArtist;
    private Label lineDot;
    private Label lineAlbum;
    private PlaylistItem shownNextItem;
    private Image shownNextImage;

    // ── Lyrics ───────────────────────────────────────────────────────────────
    private VBox lyricsPane;
    private ScrollPane lyricsScroll;
    private Label lyricsLabel;
    private Slider lyricsSizeSlider;
    private CheckBox autoFitCheck;
    private PauseTransition fitDebounce;
    private boolean fitPending;
    private boolean hasLyrics;

    // ── State ────────────────────────────────────────────────────────────────
    private PlaylistItem currentItem;
    private Duration lastCurrent = Duration.ZERO;
    private Duration lastTotal;
    private PlaybackPalette currentPalette = PlaybackPalette.DEFAULT;
    private Timeline paletteAnimation;
    private int paletteToken;

    // =========================================================================
    // Opening and closing
    // =========================================================================

    /**
     * Shows the playback window (always in the simple view) and hides the library window.
     *
     * @param libraryStage the library window, shown again when this window is closed
     */
    public void open(Stage libraryStage) {
        this.libraryStage = libraryStage;
        if (stage == null) {
            build(libraryStage);
        }
        if (stage.isShowing()) {
            stage.toFront();
            return;
        }

        stage.setMaximized(false);
        applyLayout(false);
        autoFitCheck.setSelected(true);
        bindVolume();
        refresh();

        // The new window is shown before the library is hidden, so the application never has zero windows.
        stage.show();
        stage.toFront();
        libraryStage.hide();
    }

    private void close() {
        saveBounds();
        unbindVolume();
        libraryStage.show();
        libraryStage.toFront();
        stage.hide();
    }

    // =========================================================================
    // Building the UI
    // =========================================================================

    private void build(Stage libraryStage) {
        buildTrackNodes();
        buildProgressNodes();
        buildUpNextNodes();
        buildLyricsNodes();

        root = new BorderPane();
        root.getStyleClass().add("pw-root");
        applyPalette(PlaybackPalette.DEFAULT);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource(STYLESHEET), "Missing " + STYLESHEET).toExternalForm());

        stage = new Stage();
        stage.setTitle("Now Playing");
        stage.getIcons().setAll(libraryStage.getIcons());
        stage.setScene(scene);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case SPACE -> {
                    eventPublisher.publishEvent(new PlayPauseEvent(this));
                    event.consume();
                }
                case ESCAPE -> {
                    close();
                    event.consume();
                }
                default -> {
                }
            }
        });
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        restoreBounds();

        stage.maximizedProperty().addListener((obs, was, is) -> applyLayout(is));
        stage.setOnCloseRequest(event -> {
            event.consume();
            close();
        });
        stage.showingProperty().addListener((obs, was, showing) -> {
            if (showing) {
                startTickers();
            } else {
                stopTickers();
            }
        });

        InvalidationListener boundsListener = o ->
                delayedSaveBounds.reSchedule(() -> Platform.runLater(this::saveBounds));
        stage.xProperty().addListener(boundsListener);
        stage.yProperty().addListener(boundsListener);
        stage.widthProperty().addListener(boundsListener);
        stage.heightProperty().addListener(boundsListener);

        root.widthProperty().addListener(o -> updateSizes());
        root.heightProperty().addListener(o -> updateSizes());

        audioPlayer.getPlaybackStatusProperty().addListener((obs, oldStatus, newStatus) -> onStatusChanged(newStatus));
        audioPlayer.getNextPlaylistItemProperty().addListener((obs, oldItem, newItem) -> {
            if (stage.isShowing()) {
                updateUpNext();
            }
        });
        audioPlayer.addProgressListener(this::onProgress);
    }

    private void buildTrackNodes() {
        artView = new ImageView();
        artView.setPreserveRatio(true);
        artView.setSmooth(true);

        titleLabel = label("pw-title");
        artistLabel = label("pw-artist");
        albumLabel = label("pw-album");
        dotLabel = label("pw-album", DIM);
        dotLabel.setText("•");
        dotLabel.setMinWidth(Region.USE_PREF_SIZE);
    }

    private void buildProgressNodes() {
        waveformView = new PlaybackWaveformView(waveformModel, this::seekToFraction);
        Tooltip.install(waveformView.getNode(), new Tooltip("Click to jump to a position in the track"));

        elapsedLabel = label("pw-time");
        remainingLabel = label("pw-time");
        for (Label timeLabel : List.of(elapsedLabel, remainingLabel)) {
            timeLabel.setMinWidth(Region.USE_PREF_SIZE);
            timeLabel.setAlignment(Pos.CENTER);
        }
        elapsedLabel.setText(TimeUtils.secondsToHumanTime(0));
        remainingLabel.setText("-" + TimeUtils.secondsToHumanTime(0));

        sessionLabel = label("pw-session", DIM);
        sessionLabel.setMinWidth(Region.USE_PREF_SIZE);
        sessionLabel.setTooltip(new Tooltip("Time listened in this session"));

        playPauseButton = button("▶");
        playPauseButton.setMinWidth(54);
        playPauseButton.setTooltip(new Tooltip("Play / Pause (Space)"));
        playPauseButton.setOnAction(e -> eventPublisher.publishEvent(new PlayPauseEvent(this)));

        nextButton = button("⏭");
        nextButton.setMinWidth(54);
        nextButton.setTooltip(new Tooltip("Next track"));
        nextButton.disableProperty().bind(audioPlayer.getNextPlaylistItemProperty().isNull());
        nextButton.setOnAction(e -> eventPublisher.publishEvent(new SkipNextEvent(this)));

        viewToggleButton = button("Maximize", "pw-button-text");
        viewToggleButton.setMinWidth(Region.USE_PREF_SIZE);
        viewToggleButton.setTooltip(new Tooltip("Maximize the window to show the lyrics, or return to the compact view"));
        viewToggleButton.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));

        volumeIcon = label();
        volumeIcon.setText("🔊");
        volumeIcon.setTooltip(new Tooltip("Volume"));
        volumeSlider = new Slider(0, 100, 30);
        volumeSlider.setPrefWidth(120);
        volumeSlider.setTooltip(new Tooltip("Volume"));
        volumeSlider.setOnMouseReleased(e ->
                userPreferencesService.saveVolume((int) Math.round(volumeSlider.getValue())));

        sessionTicker = new Timeline(new KeyFrame(Duration.millis(500), e -> updateSession()));
        sessionTicker.setCycleCount(Timeline.INDEFINITE);
    }

    private void buildUpNextNodes() {
        // Card (maximized view)
        cardArtView = new ImageView();
        cardArtView.setPreserveRatio(true);
        cardArtView.setSmooth(true);
        cardArtView.setFitWidth(72);
        cardArtView.setFitHeight(72);

        Label header = label("pw-small", DIM);
        header.setText("UP NEXT");
        cardTitle = label("pw-upnext-title");
        cardArtist = label("pw-artist");
        cardAlbum = label("pw-album");
        VBox cardText = new VBox(2, header, cardTitle, cardArtist, cardAlbum);
        HBox.setHgrow(cardText, Priority.ALWAYS);
        upNextCard = new HBox(12, cardArtView, cardText);
        upNextCard.setAlignment(Pos.CENTER_LEFT);

        // One-line version (simple view)
        lineThumb = new ImageView();
        lineThumb.setPreserveRatio(true);
        lineThumb.setSmooth(true);
        lineThumb.setFitWidth(24);
        lineThumb.setFitHeight(24);

        lineHeader = label("pw-small", DIM);
        lineHeader.setText("UP NEXT");
        lineHeader.setMinWidth(Region.USE_PREF_SIZE);
        lineTitle = label("pw-upnext-title");
        lineSeparator = label("pw-artist", DIM);
        lineSeparator.setText("—");
        lineSeparator.setMinWidth(Region.USE_PREF_SIZE);
        lineArtist = label("pw-artist");
        lineDot = label("pw-album", DIM);
        lineDot.setText("•");
        lineDot.setMinWidth(Region.USE_PREF_SIZE);
        lineAlbum = label("pw-album");
        upNextLine = new HBox(8, lineHeader, lineThumb, lineTitle, lineSeparator, lineArtist, lineDot, lineAlbum);
        upNextLine.setAlignment(Pos.CENTER_LEFT);
    }

    private void buildLyricsNodes() {
        lyricsLabel = new Label();
        lyricsLabel.setWrapText(true);
        lyricsLabel.setAlignment(Pos.TOP_LEFT);
        lyricsLabel.setMaxWidth(Double.MAX_VALUE);
        lyricsLabel.setPadding(new Insets(8, 12, 8, 12));
        lyricsLabel.setFont(Font.font(DEFAULT_LYRICS_SIZE));

        lyricsScroll = new ScrollPane(lyricsLabel);
        lyricsScroll.setFitToWidth(true);
        lyricsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lyricsScroll.getStyleClass().add("pw-lyrics-scroll");
        VBox.setVgrow(lyricsScroll, Priority.ALWAYS);

        lyricsSizeSlider = new Slider(MIN_LYRICS_SIZE, MAX_LYRICS_SIZE, DEFAULT_LYRICS_SIZE);
        lyricsSizeSlider.setPrefWidth(160);
        lyricsSizeSlider.setTooltip(new Tooltip("Lyrics font size"));
        // Moving the slider only changes the size; it never touches the auto-fit checkbox.
        lyricsSizeSlider.valueProperty().addListener((obs, was, size) ->
                lyricsLabel.setFont(Font.font(size.doubleValue())));

        autoFitCheck = new CheckBox("Auto-fit");
        autoFitCheck.setSelected(true);
        autoFitCheck.setFocusTraversable(false);        // like the buttons: Space always means play/pause here
        autoFitCheck.setTooltip(new Tooltip("Choose the largest font size that shows all of the lyrics"));

        Label title = label("pw-section");
        title.setText("Lyrics");
        Label sizeLabel = label("pw-small", DIM);
        sizeLabel.setText("SIZE");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, spacer, sizeLabel, lyricsSizeSlider, autoFitCheck);
        header.setAlignment(Pos.CENTER_LEFT);

        lyricsPane = new VBox(8, header, lyricsScroll);

        // Auto-fit waits for the pane to have its real size.
        fitDebounce = new PauseTransition(Duration.millis(150));
        fitDebounce.setOnFinished(e -> fitIfPending());
        InvalidationListener sizeListener = o -> {
            if (fitPending) {
                fitDebounce.playFromStart();
            }
        };
        lyricsScroll.widthProperty().addListener(sizeListener);
        lyricsScroll.heightProperty().addListener(sizeListener);
    }

    // =========================================================================
    // Layouts
    // =========================================================================

    private void applyLayout(boolean maximized) {
        this.maximized = maximized;

        // Nodes are shared between layouts: empty the old containers before reusing them.
        root.setCenter(null);
        root.setBottom(null);
        for (Pane pane : layoutPanes) {
            pane.getChildren().clear();
        }
        layoutPanes.clear();
        leftPane = null;

        root.getStyleClass().remove("maximized");
        if (maximized) {
            root.getStyleClass().add("maximized");
            applyMaximizedLayout();
        } else {
            applySimpleLayout();
        }
        updateSizes();
        requestFit();
    }

    private void applySimpleLayout() {
        titleLabel.setWrapText(false);
        viewToggleButton.setText("Maximize");
        artView.setFitWidth(SIMPLE_ART_SIZE);
        artView.setFitHeight(SIMPLE_ART_SIZE);
        waveformView.getNode().setPrefHeight(SIMPLE_WAVEFORM_HEIGHT);

        HBox metaRow = track(new HBox(8, artistLabel, dotLabel, albumLabel));
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox textBox = track(new VBox(4, titleLabel, metaRow, spacer, controlsRow()));
        HBox.setHgrow(textBox, Priority.ALWAYS);
        HBox topRow = track(new HBox(14, artView, textBox));

        VBox content = track(new VBox(10, topRow, waveformRow(), upNextLine));
        content.setPadding(new Insets(14));
        root.setCenter(content);
    }

    private void applyMaximizedLayout() {
        titleLabel.setWrapText(true);
        viewToggleButton.setText("Simple view");
        waveformView.getNode().setPrefHeight(MAXIMIZED_WAVEFORM_HEIGHT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        leftPane = track(new VBox(12, artView, titleLabel, artistLabel, albumLabel, spacer, upNextCard));
        leftPane.setPadding(new Insets(20));

        HBox.setHgrow(lyricsPane, Priority.ALWAYS);
        lyricsPane.setPadding(new Insets(20, 20, 20, 0));
        HBox center = track(new HBox(leftPane, lyricsPane));
        root.setCenter(center);

        VBox bottom = track(new VBox(10, waveformRow(), controlsRow()));
        bottom.setPadding(new Insets(0, 20, 16, 20));
        root.setBottom(bottom);
    }

    /**
     * Sizes that depend on the window size (maximized view only).
     */
    private void updateSizes() {
        if (!maximized || leftPane == null) {
            return;
        }
        double leftWidth = Math.max(280, root.getWidth() * LEFT_COLUMN_SHARE);
        leftPane.setPrefWidth(leftWidth);
        leftPane.setMinWidth(leftWidth);
        leftPane.setMaxWidth(leftWidth);

        double artSize = Math.max(120, Math.min(leftWidth - 40, root.getHeight() * 0.40));
        artView.setFitWidth(artSize);
        artView.setFitHeight(artSize);
    }

    private HBox waveformRow() {
        Node waveform = waveformView.getNode();
        HBox.setHgrow(waveform, Priority.ALWAYS);
        HBox row = track(new HBox(10, elapsedLabel, waveform, remainingLabel));
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private HBox controlsRow() {
        HBox volumeBox = track(new HBox(6, volumeIcon, volumeSlider));
        volumeBox.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = track(new HBox(10, playPauseButton, nextButton, volumeBox, spacer, sessionLabel, viewToggleButton));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private <T extends Pane> T track(T pane) {
        layoutPanes.add(pane);
        return pane;
    }

    // =========================================================================
    // Content
    // =========================================================================

    /**
     * Fills the whole window from the current state. Called when the window is opened.
     */
    private void refresh() {
        PlaylistItem item = audioPlayer.getPlaylistItemProperty().get();
        if (item != null) {
            currentItem = item;
        }
        updatePlayPauseText(audioPlayer.getPlaybackStatusProperty().get());
        if (currentItem != null) {
            showTrack(currentItem);
            lastCurrent = audioPlayer.getCurrentTime();
            lastTotal = Duration.seconds(currentItem.getDuration());
            applyProgress();
        }
    }

    private void onStatusChanged(PlaybackState status) {
        updatePlayPauseText(status);
        if (status == PlaybackState.LAUNCHING) {
            PlaylistItem item = audioPlayer.getPlaylistItemProperty().get();
            if (item != null) {
                if (stage.isShowing()) {
                    showTrack(item);
                } else {
                    currentItem = item;     // everything is filled in when the window is opened
                }
            }
        }
    }

    private void updatePlayPauseText(PlaybackState status) {
        boolean playing = status == PlaybackState.LAUNCHING || status == PlaybackState.PLAYING;
        playPauseButton.setText(playing ? "⏸" : "▶");
    }

    private void showTrack(PlaylistItem item) {
        currentItem = item;
        titleLabel.setText(item.getTitle());
        artistLabel.setText(item.getArtistName());
        albumLabel.setText(item.getAlbumName());
        artView.setImage(toImage(item.getAlbumImage(), 720));

        lastCurrent = Duration.ZERO;
        lastTotal = Duration.seconds(item.getDuration());
        applyProgress();

        showLyrics(item);
        loadPalette(item.getAlbumImage());
        updateUpNext();
    }

    private void onProgress(Duration current, Duration total) {
        lastCurrent = current;
        lastTotal = total;
        if (stage != null && stage.isShowing()) {
            applyProgress();
        }
    }

    private void applyProgress() {
        if (lastTotal == null) {
            return;
        }
        int elapsed = (int) lastCurrent.toSeconds();
        int total = (int) lastTotal.toSeconds();
        elapsedLabel.setText(TimeUtils.secondsToHumanTime(elapsed));
        remainingLabel.setText("-" + TimeUtils.secondsToHumanTime(Math.max(0, total - elapsed)));
        waveformView.setProgress(lastCurrent, lastTotal);
    }

    private void seekToFraction(double fraction) {
        if (currentItem == null || currentItem.getDuration() == null) {
            return;
        }
        audioPlayer.seek(Duration.seconds(currentItem.getDuration() * fraction));
    }

    private void updateSession() {
        String text = sessionTimer.getDisplayText();
        sessionLabel.setText(text.isEmpty() ? "" : "Session " + text);
    }

    private void updateUpNext() {
        PlaylistItem next = audioPlayer.getNextPlaylistItemProperty().get();
        boolean hasNext = next != null;

        cardArtView.setVisible(hasNext);
        cardArtView.setManaged(hasNext);
        for (Node node : List.of(lineThumb, lineSeparator, lineArtist, lineDot, lineAlbum)) {
            node.setVisible(hasNext);
            node.setManaged(hasNext);
        }
        cardArtist.setVisible(hasNext);
        cardArtist.setManaged(hasNext);
        cardAlbum.setVisible(hasNext);
        cardAlbum.setManaged(hasNext);

        if (!hasNext) {
            shownNextItem = null;
            cardTitle.setText("Nothing up next");
            lineTitle.setText("Nothing up next");
            return;
        }

        if (next != shownNextItem) {
            shownNextItem = next;
            shownNextImage = toImage(next.getAlbumImage(), 160);
        }
        cardArtView.setImage(shownNextImage);
        lineThumb.setImage(shownNextImage);

        cardTitle.setText(next.getTitle());
        lineTitle.setText(next.getTitle());
        cardArtist.setText(next.getArtistName());
        lineArtist.setText(next.getArtistName());
        cardAlbum.setText(next.getAlbumName());
        lineAlbum.setText(next.getAlbumName());

        // Parts that are the same as in the current track are dimmed.
        boolean sameArtist = currentItem != null && Objects.equals(next.getArtistId(), currentItem.getArtistId());
        boolean sameAlbum = currentItem != null && Objects.equals(next.getAlbumId(), currentItem.getAlbumId());
        setDim(cardArtist, sameArtist);
        setDim(lineArtist, sameArtist);
        setDim(cardAlbum, sameAlbum);
        setDim(lineAlbum, sameAlbum);
    }

    private static void setDim(Label label, boolean dim) {
        label.getStyleClass().remove(DIM);
        if (dim) {
            label.getStyleClass().add(DIM);
        }
    }

    // =========================================================================
    // Lyrics
    // =========================================================================

    private void showLyrics(PlaylistItem item) {
        String lyrics = LibraryLyricsTabController.normalizeNewlines(item.getLyrics());
        hasLyrics = lyrics != null && !lyrics.isBlank();
        lyricsLabel.setText(hasLyrics ? lyrics : "No lyrics");
        setDim(lyricsLabel, !hasLyrics);
        lyricsScroll.setVvalue(0);
        requestFit();       // a new track has started
    }

    /**
     * Auto-fit runs only for a new track (and when the lyrics become visible), and only when the box is checked.
     */
    private void requestFit() {
        if (!autoFitCheck.isSelected() || !hasLyrics) {
            return;
        }
        fitPending = true;
        fitDebounce.playFromStart();
    }

    private void fitIfPending() {
        if (!fitPending) {
            return;
        }
        double width = lyricsScroll.getWidth() - 40;
        double height = lyricsScroll.getHeight() - 20;
        if (!maximized || width < 50 || height < 50) {
            return;     // not laid out yet; the size listeners will try again
        }
        String text = lyricsLabel.getText();
        double size = LyricsFitter.fit(MIN_LYRICS_SIZE, MAX_LYRICS_SIZE, height,
                candidate -> measureHeight(text, candidate, width));
        fitPending = false;
        lyricsSizeSlider.setValue(size);
    }

    private static double measureHeight(String text, double fontSize, double wrappingWidth) {
        Text measuring = new Text(text);
        measuring.setFont(Font.font(fontSize));
        measuring.setWrappingWidth(wrappingWidth);
        return measuring.getLayoutBounds().getHeight();
    }

    // =========================================================================
    // Colors from album art
    // =========================================================================

    private void loadPalette(byte[] image) {
        int token = ++paletteToken;
        if (image == null || image.length == 0) {
            moveToPalette(PlaybackPalette.DEFAULT);
            return;
        }
        Thread thread = new Thread(() -> {
            PlaybackPalette palette = PlaybackPalette.fromImage(image);
            Platform.runLater(() -> {
                if (token == paletteToken) {
                    moveToPalette(palette);
                }
            });
        }, "Album art palette");
        thread.setDaemon(true);
        thread.start();
    }

    private void moveToPalette(PlaybackPalette target) {
        if (paletteAnimation != null) {
            paletteAnimation.stop();
            paletteAnimation = null;
        }
        if (!stage.isShowing()) {
            applyPalette(target);
            return;
        }

        PlaybackPalette from = currentPalette;
        DoubleProperty progress = new SimpleDoubleProperty(0);
        progress.addListener((obs, was, t) -> applyPalette(from.interpolate(target, t.doubleValue())));
        paletteAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progress, 0)),
                new KeyFrame(Duration.millis(700), new KeyValue(progress, 1, Interpolator.EASE_BOTH))
        );
        paletteAnimation.setOnFinished(e -> applyPalette(target));
        paletteAnimation.play();
    }

    private void applyPalette(PlaybackPalette palette) {
        currentPalette = palette;
        root.setStyle(palette.toCssVariables());
        waveformView.setPalette(palette);
    }

    // =========================================================================
    // Volume, tickers, window bounds
    // =========================================================================

    private void bindVolume() {
        if (!volumeBound) {
            volumeSlider.valueProperty().bindBidirectional(audioPlayer.getVolumeProperty());
            volumeBound = true;
        }
    }

    private void unbindVolume() {
        if (volumeBound) {
            volumeSlider.valueProperty().unbindBidirectional(audioPlayer.getVolumeProperty());
            volumeBound = false;
        }
    }

    private void startTickers() {
        updateSession();
        sessionTicker.play();
        waveformView.start();
    }

    private void stopTickers() {
        sessionTicker.stop();
        waveformView.stop();
        fitDebounce.stop();
    }

    private void restoreBounds() {
        double width = UserPreferencesService.DEFAULT_PLAYBACK_WINDOW_WIDTH;
        double height = UserPreferencesService.DEFAULT_PLAYBACK_WINDOW_HEIGHT;
        double[] saved = userPreferencesService.getPlaybackWindowBounds();

        boolean positioned = false;
        if (saved != null) {
            width = Math.max(MIN_WIDTH, saved[2]);
            height = Math.max(MIN_HEIGHT, saved[3]);
            // Ignore a saved position if that monitor is not connected any more.
            if (!Screen.getScreensForRectangle(saved[0], saved[1], width, height).isEmpty()) {
                stage.setX(saved[0]);
                stage.setY(saved[1]);
                positioned = true;
            }
        }
        stage.setWidth(width);
        stage.setHeight(height);
        if (!positioned) {
            stage.centerOnScreen();
        }
    }

    /**
     * Only the simple view is remembered.
     */
    private void saveBounds() {
        if (stage == null || !stage.isShowing() || stage.isMaximized() || stage.isIconified()) {
            return;
        }
        userPreferencesService.savePlaybackWindowBounds(
                stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    // =========================================================================
    // Small helpers
    // =========================================================================

    private static Label label(String... styleClasses) {
        Label label = new Label();
        label.getStyleClass().addAll(styleClasses);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        return label;
    }

    private static Button button(String text, String... extraStyleClasses) {
        Button button = new Button(text);
        button.getStyleClass().add("pw-button");
        button.getStyleClass().addAll(extraStyleClasses);
        button.setFocusTraversable(false);
        return button;
    }

    private static Image toImage(byte[] data, double maxSize) {
        if (data != null && data.length > 0) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
                Image image = new Image(in, maxSize, maxSize, true, true);
                if (!image.isError()) {
                    return image;
                }
            } catch (Exception e) {
                log.warn("Unable to display album art", e);
            }
        }
        return placeholder();
    }

    private static Image placeholder() {
        if (placeholderImage == null) {
            placeholderImage = new Image(PlaybackWindow.class.getResourceAsStream(NO_IMAGE));
        }
        return placeholderImage;
    }
}
