package com.github.curiousoddman.curious_tunes.ui.controller.element.tabs;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackOverridesHistoryRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.domain.lyrics.LyricsService;
import com.github.curiousoddman.curious_tunes.event.AlbumCoverChangedEvent;
import com.github.curiousoddman.curious_tunes.event.UserNotificationEvent;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.util.ConversionUtils;
import com.github.curiousoddman.curious_tunes.util.ImageBytes;
import com.github.curiousoddman.curious_tunes.util.ImageUtils;
import com.github.curiousoddman.curious_tunes.util.styles.CssClasses;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.TableField;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;
import static com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryLyricsTabController.normalizeNewlines;
import static com.github.curiousoddman.curious_tunes.util.ConversionUtils.str;

/**
 * Edits the tags of one track, or of several tracks at once (only the fields that are changed are applied to all).
 * <p>
 * The tab keeps track of what the user changed (blue border), what was edited earlier (orange border, with a button
 * that restores the original value) and what is invalid. Save is possible only when something changed.
 * <p>
 * Everything that only concerns the screen is skipped while the controls of the header, the buttons and so on are
 * not there ({@code uiReady}), so that the saving logic can be tested with a few text fields.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryTagEditTabController implements Initializable {
    private static final Pattern DATE_PATTERN     = Pattern.compile("\\d{4}(-\\d{2}(-\\d{2})?)?");
    private static final String  DIFFERENT_VALUES = "(different values)";

    @FXML
    public GridPane  tagsGrid;
    @FXML
    public TextField artistField;
    @FXML
    public TextField albumField;
    @FXML
    public TextField titleField;
    @FXML
    public TextField trackNumberField;
    @FXML
    public TextField releaseDateField;
    @FXML
    public TextField diskNumberField;
    @FXML
    public TextField genreField;
    @FXML
    public TextField composerField;
    @FXML
    public ImageView albumCoverImage;
    @FXML
    public TextArea  lyricsEditArea;
    @FXML
    public VBox      rootPane;

    @FXML
    public Label     emptyLabel;
    @FXML
    public VBox      contentBox;
    @FXML
    public ImageView headerCover;
    @FXML
    public Label     headerTitle;
    @FXML
    public Label     headerSubtitle;
    @FXML
    public Label     headerPath;
    @FXML
    public Button    saveButton;
    @FXML
    public Button    revertButton;
    @FXML
    public Button    findLyricsButton;
    @FXML
    public Button    restoreTitleButton;
    @FXML
    public Button    restoreTrackNumberButton;
    @FXML
    public Button    restoreDiskNumberButton;
    @FXML
    public Button    restoreGenreButton;
    @FXML
    public Button    restoreComposerButton;
    @FXML
    public Button    restoreReleaseDateButton;
    @FXML
    public Button    restoreLyricsButton;
    @FXML
    public HBox      coverRow;
    @FXML
    public Label     legendLabel;

    private final LyricsService             lyricsService;
    private final DataAccess                dataAccess;
    private final ApplicationEventPublisher eventPublisher;

    private       TrackInfo                         trackInfo;                         // the track in single mode
    private       List<TrackInfo>                   batchTracks    = List.of();        // several tracks in batch mode
    private       List<TrackOverridesHistoryRecord> trackOverrides = List.of();
    private final Map<TextInputControl, String>     originals      = new IdentityHashMap<>();   // texts when loaded
    private final Map<TextInputControl, String>     defaultPrompts = new IdentityHashMap<>();
    private       byte[]                            pendingCover;
    private       Image                             noCoverImage;
    private       boolean                           uiReady;
    private       boolean                           loading;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        noCoverImage = albumCoverImage.getImage();
        for (TextInputControl control : editableControls()) {
            defaultPrompts.put(control, control.getPromptText());
        }

        // Track and disc number: digits only.
        for (TextField numberField : List.of(trackNumberField, diskNumberField)) {
            numberField.setTextFormatter(new TextFormatter<String>(
                    change -> change.getControlNewText()
                                    .matches("\\d{0,4}") ? change : null));
        }

        for (TextInputControl control : editableControls()) {
            control.textProperty()
                   .addListener((obs, oldText, newText) -> {
                       if (!loading) {
                           refreshUi();
                       }
                   });
        }

        // Enter moves to the next field.
        List<TextField> order = List.of(artistField, albumField, titleField, trackNumberField, diskNumberField,
                genreField, composerField, releaseDateField);
        for (int i = 0; i < order.size(); i++) {
            Node next = i + 1 < order.size() ? order.get(i + 1) : lyricsEditArea;
            order.get(i)
                 .setOnAction(_ -> next.requestFocus());
        }

        rootPane.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.S && !saveButton.isDisabled()) {
                onSave(null);
                event.consume();
            }
        });

        coverRow.setOnDragOver(this::onCoverDragOver);
        coverRow.setOnDragDropped(this::onCoverDropped);

        legendLabel.setText("Blue border: changed, not saved yet. Orange border: edited earlier; the undo button next "
                + "to it restores the original value.");
        uiReady = true;
        refreshUi();
    }

    // =========================================================================
    // Loading
    // =========================================================================

    public void showTags(TrackInfo trackInfo) {
        this.trackInfo = trackInfo;
        this.batchTracks = List.of();
        this.pendingCover = null;

        loading = true;
        try {
            fillFieldsFromTrackInfo();
        } finally {
            loading = false;
        }
        markModifiedFields();
        captureOriginals();
        refreshHeader();
        refreshUi();
    }

    /**
     * Several tracks: the fields show what the tracks have in common; the fields that are changed are applied to all.
     */
    public void showTags(List<TrackInfo> tracks) {
        if (tracks.size() == 1) {
            showTags(tracks.getFirst());
            return;
        }
        this.trackInfo = null;
        this.batchTracks = List.copyOf(tracks);
        this.trackOverrides = List.of();
        this.pendingCover = null;

        loading = true;
        try {
            fillFieldsForBatch();
        } finally {
            loading = false;
        }
        clearOverrideMarks();
        captureOriginals();
        refreshHeader();
        refreshUi();
    }

    private boolean isBatch() {
        return batchTracks.size() > 1;
    }

    private List<TrackInfo> tracksBeingEdited() {
        return isBatch() ? batchTracks : (trackInfo == null ? List.of() : List.of(trackInfo));
    }

    private void fillFieldsFromTrackInfo() {
        artistField.setText(trackInfo.getArtistName());
        albumField.setText(trackInfo.getAlbumName());
        titleField.setText(trackInfo.getTitle());
        trackNumberField.setText(str(trackInfo.getTrackNumber()));
        diskNumberField.setText(str(trackInfo.getDiskNumber()));
        genreField.setText(trackInfo.getGenre());
        composerField.setText(trackInfo.getComposer());
        releaseDateField.setText(trackInfo.getReleaseDate());
        lyricsEditArea.setText(trackInfo.getLyrics());
        for (TextInputControl control : editableControls()) {
            control.setPromptText(defaultPrompts.get(control));
        }
    }

    private void fillFieldsForBatch() {
        fillCommon(artistField, batchTracks.stream()
                                           .map(TrackInfo::getArtistName));
        fillCommon(albumField, batchTracks.stream()
                                          .map(TrackInfo::getAlbumName));
        fillCommon(diskNumberField, batchTracks.stream()
                                               .map(t -> str(t.getDiskNumber())));
        fillCommon(genreField, batchTracks.stream()
                                          .map(TrackInfo::getGenre));
        fillCommon(composerField, batchTracks.stream()
                                             .map(TrackInfo::getComposer));
        fillCommon(releaseDateField, batchTracks.stream()
                                                .map(TrackInfo::getReleaseDate));
        titleField.setText("");
        trackNumberField.setText("");
        lyricsEditArea.setText("");
    }

    private void fillCommon(TextField field, Stream<String> values) {
        List<String> distinct = values.map(value -> value == null ? "" : value)
                                      .distinct()
                                      .toList();
        if (distinct.size() == 1) {
            field.setText(distinct.getFirst());
            field.setPromptText(defaultPrompts.get(field));
        } else {
            field.setText("");
            field.setPromptText(DIFFERENT_VALUES);
        }
    }

    private List<TextInputControl> editableControls() {
        return List.of(artistField, albumField, titleField, trackNumberField, diskNumberField, genreField, composerField,
                releaseDateField, lyricsEditArea);
    }

    private void captureOriginals() {
        originals.clear();
        for (TextInputControl control : editableControls()) {
            originals.put(control, textOf(control));
        }
    }

    private static String textOf(TextInputControl control) {
        return control.getText() == null ? "" : control.getText();
    }

    private boolean isChanged(TextInputControl control) {
        String original = originals.get(control);
        return original != null && !Objects.equals(textOf(control), original);
    }

    // =========================================================================
    // Screen state
    // =========================================================================

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private static void toggleClass(Node node, String styleClass, boolean on) {
        node.getStyleClass()
            .remove(styleClass);
        if (on) {
            node.getStyleClass()
                .add(styleClass);
        }
    }

    /**
     * Header and cover pictures; only when the tracks or the cover change (not on every key).
     */
    private void refreshHeader() {
        if (!uiReady) {
            return;
        }
        boolean has = trackInfo != null || isBatch();
        if (!has) {
            return;
        }
        if (isBatch()) {
            headerTitle.setText(batchTracks.size() + " tracks selected");
            headerSubtitle.setText("Only the fields you change are applied to all of them.");
            headerPath.setText("");
            headerCover.setImage(noCoverImage);
        } else {
            headerTitle.setText(trackInfo.getTitle());
            headerSubtitle.setText(trackInfo.getArtistName() + " — " + trackInfo.getAlbumName());
            headerPath.setText(trackInfo.getFileLocation());
            headerPath.setTooltip(trackInfo.getFileLocation() == null ? null : new Tooltip(trackInfo.getFileLocation()));
            showCover(headerCover, trackInfo.getAlbumImage());
        }
        if (pendingCover != null) {
            showCover(albumCoverImage, pendingCover);
        } else if (isBatch()) {
            albumCoverImage.setImage(noCoverImage);
        } else {
            showCover(albumCoverImage, trackInfo.getAlbumImage());
        }
    }

    private void showCover(ImageView view, byte[] bytes) {
        view.setImage(noCoverImage);
        if (bytes != null && bytes.length > 0) {
            ImageUtils.setImageIfPresent(bytes, view);
        }
    }

    /**
     * Which fields are changed or invalid, whether Save and Revert are possible, the restore buttons.
     */
    private void refreshUi() {
        if (!uiReady) {
            return;
        }
        boolean has = trackInfo != null || isBatch();
        setShown(emptyLabel, !has);
        setShown(contentBox, has);
        if (!has) {
            return;
        }

        boolean batch = isBatch();
        for (TextInputControl control : List.of(titleField, trackNumberField, lyricsEditArea)) {
            control.setDisable(batch);        // each track has its own title, number and lyrics
        }
        findLyricsButton.setDisable(batch);

        boolean dirty = pendingCover != null;
        boolean valid = true;
        for (TextInputControl control : editableControls()) {
            boolean changed = !control.isDisabled() && isChanged(control);
            boolean invalid = changed && !isValid(control);
            toggleClass(control, CssClasses.FIELD_CHANGED, changed && !invalid);
            toggleClass(control, CssClasses.FIELD_INVALID, invalid);
            dirty |= changed;
            valid &= !invalid;
        }
        releaseDateField.setTooltip(releaseDateField.getStyleClass()
                                                    .contains(CssClasses.FIELD_INVALID)
                ? new Tooltip("Use YYYY, YYYY-MM or YYYY-MM-DD")
                : null);
        saveButton.setDisable(!(dirty && valid));
        revertButton.setDisable(!dirty);
        updateRestoreButtons();
    }

    /**
     * Numbers are limited while typing; a changed release date has to look like a date.
     */
    private boolean isValid(TextInputControl control) {
        if (control == releaseDateField) {
            String text = textOf(control).trim();
            return text.isEmpty() || DATE_PATTERN.matcher(text)
                                                 .matches();
        }
        return true;
    }

    private void updateRestoreButtons() {
        boolean single = !isBatch() && trackInfo != null;
        updateRestoreButton(restoreTitleButton, titleField, single ? trackInfo.getTitle() : null, TRACK.TITLE, single);
        updateRestoreButton(restoreTrackNumberButton, trackNumberField, single ? trackInfo.getTrackNumber() : null,
                TRACK.TRACK_NUMBER, single);
        updateRestoreButton(restoreDiskNumberButton, diskNumberField, single ? trackInfo.getDiskNumber() : null,
                TRACK.DISK_NUMBER, single);
        updateRestoreButton(restoreGenreButton, genreField, single ? trackInfo.getGenre() : null, TRACK.GENRE, single);
        updateRestoreButton(restoreComposerButton, composerField, single ? trackInfo.getComposer() : null,
                TRACK.COMPOSER, single);
        updateRestoreButton(restoreReleaseDateButton, releaseDateField, single ? trackInfo.getReleaseDate() : null,
                TRACK.RELEASE_DATE, single);
        updateRestoreButton(restoreLyricsButton, lyricsEditArea, single ? trackInfo.getLyrics() : null, TRACK.LYRICS, single);
    }

    /**
     * The undo button of a field that was edited earlier: it puts the original value back into the field.
     */
    private void updateRestoreButton(Button button,
                                     TextInputControl control,
                                     Object savedValue,
                                     TableField<TrackRecord, ?> field,
                                     boolean single) {
        Optional<TrackOverridesHistoryRecord> first   = single ? firstOverride(trackOverrides, field) : Optional.empty();
        boolean                               visible = first.isPresent() && wasOverridden(savedValue, trackOverrides, field);
        setShown(button, visible);
        if (!visible) {
            return;
        }
        String original = first.get()
                               .getOldValue() == null ? "" : first.get()
                                                                  .getOldValue();
        button.setDisable(Objects.equals(textOf(control), original));
        button.setTooltip(new Tooltip("Restore the original value" + (original.isEmpty() ? " (empty)" : ": " + abbreviate(original))));
        button.setOnAction(_ -> control.setText(original));
    }

    private static String abbreviate(String text) {
        String oneLine = text.replaceAll("\\s+", " ");
        return oneLine.length() > 60 ? oneLine.substring(0, 57) + "…" : oneLine;
    }

    // =========================================================================
    // Lyrics, revert, cover
    // =========================================================================

    @FXML
    public void onFindLyrics(ActionEvent actionEvent) {
        if (trackInfo == null) {
            return;
        }
        lyricsService.findLyricsAsync(
                trackInfo.getTrackArtist()
                         .getName(),
                trackInfo.getTrackAlbum()
                         .getName(),
                trackInfo.getTitle(),
                lyrics -> Platform.runLater(() -> {
                    lyricsEditArea.setText(normalizeNewlines(lyrics));
                    eventPublisher.publishEvent(UserNotificationEvent.info(this, "Lyrics found. Review them and press Save."));
                }),
                () -> {
                    log.error("Unable to find lyrics online");
                    eventPublisher.publishEvent(UserNotificationEvent.error(this, "No lyrics found online for this track"));
                }
        );
    }

    @FXML
    public void onRevert(ActionEvent actionEvent) {
        loading = true;
        try {
            originals.forEach(TextInputControl::setText);
        } finally {
            loading = false;
        }
        pendingCover = null;
        refreshHeader();
        refreshUi();
    }

    @FXML
    public void onChooseCover(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose the album cover");
        chooser.getExtensionFilters()
               .add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File file = chooser.showOpenDialog(rootPane.getScene()
                                                   .getWindow());
        if (file != null) {
            useCoverFile(file);
        }
    }

    @FXML
    public void onPasteCover(ActionEvent actionEvent) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        try {
            if (clipboard.hasFiles()) {
                File file = clipboard.getFiles()
                                     .getFirst();
                if (ImageBytes.isImageFile(file)) {
                    useCoverFile(file);
                    return;
                }
            }
            if (clipboard.hasImage()) {
                setPendingCover(ImageBytes.toPng(clipboard.getImage()));
                return;
            }
        } catch (Exception e) {
            log.warn("Unable to paste the cover", e);
        }
        eventPublisher.publishEvent(UserNotificationEvent.error(this, "There is no image on the clipboard"));
    }

    private void onCoverDragOver(DragEvent event) {
        if (event.getDragboard()
                 .hasFiles() && ImageBytes.isImageFile(event.getDragboard()
                                                            .getFiles()
                                                            .getFirst())) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void onCoverDropped(DragEvent event) {
        boolean done = false;
        if (event.getDragboard()
                 .hasFiles()) {
            File file = event.getDragboard()
                             .getFiles()
                             .getFirst();
            if (ImageBytes.isImageFile(file)) {
                useCoverFile(file);
                done = true;
            }
        }
        event.setDropCompleted(done);
        event.consume();
    }

    private void useCoverFile(File file) {
        try {
            setPendingCover(ImageBytes.read(file));
        } catch (Exception e) {
            log.warn("Unable to read {}", file, e);
            eventPublisher.publishEvent(UserNotificationEvent.error(this, "Unable to read the image: " + e.getMessage()));
        }
    }

    private void setPendingCover(byte[] bytes) {
        if (trackInfo == null && !isBatch()) {
            return;
        }
        if (ImageBytes.toImage(bytes) == null) {
            eventPublisher.publishEvent(UserNotificationEvent.error(this, "This is not an image"));
            return;
        }
        pendingCover = bytes;
        refreshHeader();
        refreshUi();
    }

    // =========================================================================
    // Saving
    // =========================================================================

    record ChangeCheck<T>(T trackValue, String uiValue, TableField<TrackRecord, T> field, Function<String, T> mapper,
                          TextInputControl source) {

    }

    @FXML
    public void onSave(ActionEvent actionEvent) {
        if (trackInfo == null && !isBatch()) {
            eventPublisher.publishEvent(UserNotificationEvent.error(this, "Choose a track first: right-click it and select \"Edit tags\""));
            return;
        }
        try {
            if (isBatch()) {
                for (TrackInfo track : batchTracks) {
                    saveTrack(track, true);
                }
            } else {
                saveTrack(trackInfo, false);
            }
            Set<Integer> coverAlbums = savePendingCover();
            if (isBatch()) {
                showTags(batchTracks);        // new starting point for the next change
            } else {
                markModifiedFields();
                captureOriginals();
                refreshHeader();
                refreshUi();
            }
            if (!coverAlbums.isEmpty()) {
                eventPublisher.publishEvent(new AlbumCoverChangedEvent(this, coverAlbums));
            }
            eventPublisher.publishEvent(UserNotificationEvent.info(this, "Tags saved"));
        } catch (Exception e) {
            log.error("Unable to save tags", e);
            eventPublisher.publishEvent(UserNotificationEvent.error(this, "Unable to save tags: " + e.getMessage()));
        }
    }

    /**
     * The new cover is stored in the albums the edited tracks belong to (after any change of album).
     */
    private Set<Integer> savePendingCover() {
        if (pendingCover == null) {
            return Set.of();
        }
        Set<Integer> albumIds = new HashSet<>();
        for (TrackInfo track : tracksBeingEdited()) {
            Integer albumId = track.getTrackRecord()
                                   .getFkAlbum();
            if (albumId != null && albumIds.add(albumId)) {
                AlbumRecord album = dataAccess.getAlbum(albumId);
                album.setImage(pendingCover);
                album.update();
            }
        }
        pendingCover = null;
        return albumIds;
    }

    private static boolean sameValue(String uiValue, String trackValue) {
        return Objects.equals(blankToNull(uiValue), blankToNull(trackValue));
    }

    private static String blankToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * @param batch true: only the fields that were changed in the editor are applied
     */
    @SneakyThrows
    private void saveTrack(TrackInfo track, boolean batch) {
        log.info("Updating track info \n{}", track);
        TrackRecord trackRecord = track.getTrackRecord();

        ArtistRecord artist        = track.getTrackArtist();
        boolean      artistChanged = batch ? isChanged(artistField) : !artistField.getText()
                                                                                  .equals(track.getArtistName());
        boolean      albumChanged  = batch ? isChanged(albumField) : !albumField.getText()
                                                                                .equals(track.getAlbumName());
        String       albumName     = batch && !albumChanged ? track.getAlbumName() : albumField.getText();
        if (artistChanged) {
            log.info("Artist name changed!");
            artist = dataAccess.getOrInsertArtist(artistField.getText());
            AlbumRecord album = dataAccess.getOrInsertAlbum(artist.getId(), albumName, track.getAlbumImage());
            dataAccess.storeTrackOverride(trackRecord, TRACK.FK_ALBUM, str(trackRecord.getFkAlbum()));
            trackRecord.setFkAlbum(album.getId());
            trackRecord.update(TRACK.FK_ALBUM);
        } else if (albumChanged) {
            log.info("Album changed");
            AlbumRecord album = dataAccess.getOrInsertAlbum(artist.getId(), albumName, track.getAlbumImage());
            dataAccess.storeTrackOverride(trackRecord, TRACK.FK_ALBUM, str(trackRecord.getFkAlbum()));
            trackRecord.setFkAlbum(album.getId());
            trackRecord.update(TRACK.FK_ALBUM);
        }

        List<ChangeCheck<?>> changeChecks = List.of(
                new ChangeCheck<>(track.getTitle(), titleField.getText(), TRACK.TITLE, Function.identity(), titleField),
                new ChangeCheck<>(track.getTrackNumber(), trackNumberField.getText(), TRACK.TRACK_NUMBER, ConversionUtils::toInteger, trackNumberField),
                new ChangeCheck<>(track.getDiskNumber(), diskNumberField.getText(), TRACK.DISK_NUMBER, ConversionUtils::toInteger, diskNumberField),
                new ChangeCheck<>(track.getGenre(), genreField.getText(), TRACK.GENRE, Function.identity(), genreField),
                new ChangeCheck<>(track.getComposer(), composerField.getText(), TRACK.COMPOSER, Function.identity(), composerField),
                new ChangeCheck<>(track.getReleaseDate(), releaseDateField.getText(), TRACK.RELEASE_DATE, Function.identity(), releaseDateField),
                new ChangeCheck<>(track.getLyrics(), lyricsEditArea.getText(), TRACK.LYRICS, Function.identity(), lyricsEditArea)
        );

        Predicate<ChangeCheck<?>> applies = check -> !batch || (!check.source()
                                                                      .isDisabled() && isChanged(check.source()));
        for (ChangeCheck changeCheck : changeChecks) {
            if (applies.test(changeCheck) && !sameValue(changeCheck.uiValue, str(changeCheck.trackValue))) {
                log.info("Value for '{}' does not match '{}' --> '{}', updating...", changeCheck.field.getName(), changeCheck.uiValue, changeCheck.trackValue);
                dataAccess.storeTrackOverride(trackRecord, changeCheck.field, str(changeCheck.trackValue));
                trackRecord.set(changeCheck.field, changeCheck.mapper.apply(changeCheck.uiValue));
                trackRecord.update(changeCheck.field);
            }
        }
    }

    // =========================================================================
    // Edited earlier
    // =========================================================================

    private void markModifiedFields() {
        trackOverrides = dataAccess.getTrackOverrides(trackInfo.getTrackId());

        // How to detect?
        //markIfDifferent(artistField, trackInfo.getArtistId(), trackOverrides, TRACK.FK_ALBUM);
        markIfDifferent(albumField, trackInfo.getAlbumId(), trackOverrides, TRACK.FK_ALBUM);
        markIfDifferent(titleField, trackInfo.getTitle(), trackOverrides, TRACK.TITLE);
        markIfDifferent(trackNumberField, trackInfo.getTrackNumber(), trackOverrides, TRACK.TRACK_NUMBER);
        markIfDifferent(diskNumberField, trackInfo.getDiskNumber(), trackOverrides, TRACK.DISK_NUMBER);
        markIfDifferent(genreField, trackInfo.getGenre(), trackOverrides, TRACK.GENRE);
        markIfDifferent(composerField, trackInfo.getComposer(), trackOverrides, TRACK.COMPOSER);
        markIfDifferent(releaseDateField, trackInfo.getReleaseDate(), trackOverrides, TRACK.RELEASE_DATE);
        markIfDifferent(lyricsEditArea, trackInfo.getLyrics(), trackOverrides, TRACK.LYRICS);
    }

    private void clearOverrideMarks() {
        for (TextInputControl control : editableControls()) {
            control.getStyleClass()
                   .remove(CssClasses.FIELD_OVERRIDDEN);
        }
    }

    private void markIfDifferent(TextInputControl inputField,
                                 Object currentValue,
                                 List<TrackOverridesHistoryRecord> trackOverrides,
                                 TableField<TrackRecord, ?> field) {
        boolean wasOverridden = wasOverridden(currentValue, trackOverrides, field);
        inputField.getStyleClass()
                  .remove(CssClasses.FIELD_OVERRIDDEN);
        if (wasOverridden) {
            inputField.getStyleClass()
                      .add(CssClasses.FIELD_OVERRIDDEN);
        }
    }

    /**
     * The first override of the field: its old value is the value that came from the file.
     */
    static Optional<TrackOverridesHistoryRecord> firstOverride(List<TrackOverridesHistoryRecord> trackOverrides,
                                                               TableField<TrackRecord, ?> field) {
        return trackOverrides.stream()
                             .filter(r -> r.getField()
                                           .equals(field.getName()))
                             .findFirst();
    }

    /**
     * Is the current value different from the original one (the one that was replaced by the first edit)?
     * A field that was edited and later put back to its original value is not "overridden".
     */
    public static boolean wasOverridden(Object currentValue, List<TrackOverridesHistoryRecord> trackOverrides, TableField<TrackRecord, ?> field) {
        Optional<TrackOverridesHistoryRecord> first = firstOverride(trackOverrides, field);
        return first.isPresent()
                && !Objects.equals(str(currentValue), first.get()
                                                           .getOldValue());
    }
}
