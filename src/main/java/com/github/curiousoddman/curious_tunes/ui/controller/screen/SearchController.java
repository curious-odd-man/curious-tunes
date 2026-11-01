package com.github.curiousoddman.curious_tunes.ui.controller.screen;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.ShowAlbumInLibraryEvent;
import com.github.curiousoddman.curious_tunes.event.ShowInLibraryEvent;
import com.github.curiousoddman.curious_tunes.event.UserNotificationEvent;
import com.github.curiousoddman.curious_tunes.model.SearchResult;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistAddMode;
import com.github.curiousoddman.curious_tunes.util.TrackActions;
import com.github.curiousoddman.curious_tunes.util.UiIcons;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Search of the whole library. Type, pick with the arrow keys, then Enter plays, Shift+Enter adds to the queue and
 * Ctrl+Enter shows the hit in the library.
 */
@Slf4j
@Lazy
@Component
@RequiredArgsConstructor
public class SearchController implements Initializable {
    private static final int MIN_LENGTH     = 2;
    private static final int LIMIT_PER_KIND = 25;

    private final DataAccess                dataAccess;
    private final UserPreferencesService    userPreferencesService;
    private final ApplicationEventPublisher eventPublisher;

    @FXML
    public TextField              queryField;
    @FXML
    public ListView<SearchResult> resultsList;
    @FXML
    public Label                  emptyLabel;
    @FXML
    public ProgressIndicator      progress;

    private final PauseTransition debounce  = new PauseTransition(Duration.millis(250));
    private final AtomicInteger   requestId = new AtomicInteger();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        requestId.incrementAndGet();
        resultsList.getItems()
                   .clear();
        resultsList.setCellFactory(_ -> new ResultCell());
        resultsList.setPlaceholder(new Label(""));
        showHint("Type at least " + MIN_LENGTH + " letters.");

        debounce.setOnFinished(_ -> search());
        queryField.textProperty()
                  .addListener((obs, oldText, newText) -> debounce.playFromStart());

        queryField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DOWN) {
                resultsList.requestFocus();
                if (resultsList.getSelectionModel()
                               .isEmpty() && !resultsList.getItems()
                                                         .isEmpty()) {
                    resultsList.getSelectionModel()
                               .selectFirst();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                activate(event);
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            }
        });
        resultsList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                activate(event);
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.UP && resultsList.getSelectionModel()
                                                                   .getSelectedIndex() <= 0) {
                queryField.requestFocus();
                event.consume();
            }
        });
        resultsList.setOnMouseClicked(event -> {
            if (event.getClickCount() > 1) {
                act(Action.PLAY);
            }
        });
        Platform.runLater(() -> queryField.requestFocus());
    }

    private enum Action {PLAY, ADD, SHOW}

    private void activate(KeyEvent event) {
        act(event.isShortcutDown() ? Action.SHOW : event.isShiftDown() ? Action.ADD : Action.PLAY);
    }

    private void showHint(String text) {
        emptyLabel.setText(text);
        emptyLabel.setVisible(!text.isEmpty());
        emptyLabel.setManaged(!text.isEmpty());
    }

    // =========================================================================
    // Searching
    // =========================================================================

    private void search() {
        String text = queryField.getText() == null ? "" : queryField.getText()
                                                                    .trim();
        int    id   = requestId.incrementAndGet();
        if (text.length() < MIN_LENGTH) {
            resultsList.getItems()
                       .clear();
            progress.setVisible(false);
            progress.setManaged(false);
            showHint("Type at least " + MIN_LENGTH + " letters.");
            return;
        }
        progress.setVisible(true);
        progress.setManaged(true);
        Thread t = new Thread(() -> {
            try {
                List<SearchResult> found = dataAccess.searchLibrary(text, LIMIT_PER_KIND);
                Platform.runLater(() -> {
                    if (id != requestId.get()) {
                        return;     // a newer search was started
                    }
                    progress.setVisible(false);
                    progress.setManaged(false);
                    resultsList.getItems()
                               .setAll(found);
                    showHint(found.isEmpty() ? "Nothing found for “" + text + "”" : "");
                    if (!found.isEmpty()) {
                        resultsList.getSelectionModel()
                                   .selectFirst();
                    }
                });
            } catch (Exception e) {
                log.error("Search failed", e);
                Platform.runLater(() -> {
                    progress.setVisible(false);
                    progress.setManaged(false);
                    eventPublisher.publishEvent(UserNotificationEvent.error(this, "The search failed"));
                });
            }
        }, "Search the library");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // Actions on the selected hit
    // =========================================================================

    private void act(Action action) {
        SearchResult result = resultsList.getSelectionModel()
                                         .getSelectedItem();
        if (result == null) {
            return;
        }
        Shuffle shuffleWhenReplacing = userPreferencesService.isPlayShuffled() ? Shuffle.AFTER_ADDING_TO_PLAYLIST : Shuffle.SKIP;
        Shuffle shuffleWhenAdding    = userPreferencesService.isPlayShuffled() ? Shuffle.BEFORE_ADDING_TO_PLAYLIST : Shuffle.SKIP;
        switch (result.kind()) {
            case ARTIST -> {
                switch (action) {
                    case PLAY -> publishAdd(AddToPlaylistEvent.builder()
                                                              .artistRecord(dataAccess.getArtist(result.id()))
                                                              .shuffle(shuffleWhenReplacing)
                                                              .playlistAddMode(PlaylistAddMode.REPLACE));
                    case ADD -> publishAdd(AddToPlaylistEvent.builder()
                                                             .artistRecord(dataAccess.getArtist(result.id()))
                                                             .shuffle(shuffleWhenAdding)
                                                             .playlistAddMode(PlaylistAddMode.APPEND));
                    case SHOW ->
                            eventPublisher.publishEvent(new ShowAlbumInLibraryEvent(this, result.artistId(), null));
                }
            }
            case ALBUM -> {
                AlbumRecord album = dataAccess.getAlbum(result.id());
                switch (action) {
                    case PLAY -> publishAdd(AddToPlaylistEvent.builder()
                                                              .albums(List.of(album))
                                                              .shuffle(shuffleWhenReplacing)
                                                              .playlistAddMode(PlaylistAddMode.REPLACE));
                    case ADD -> publishAdd(AddToPlaylistEvent.builder()
                                                             .albums(List.of(album))
                                                             .shuffle(shuffleWhenAdding)
                                                             .playlistAddMode(PlaylistAddMode.APPEND));
                    case SHOW ->
                            eventPublisher.publishEvent(new ShowAlbumInLibraryEvent(this, result.artistId(), result.albumId()));
                }
            }
            case TRACK -> {
                TrackInfo track = findTrack(result);
                if (track == null) {
                    eventPublisher.publishEvent(UserNotificationEvent.error(this, "This track is not in the library any more"));
                    return;
                }
                switch (action) {
                    case PLAY -> TrackActions.playNow(eventPublisher, this, List.of(track));
                    case ADD -> TrackActions.addToQueue(eventPublisher, this, List.of(track));
                    case SHOW -> eventPublisher.publishEvent(new ShowInLibraryEvent(this, track));
                }
            }
        }
        if (action == Action.ADD) {
            eventPublisher.publishEvent(UserNotificationEvent.info(this, "Added to the queue: " + result.title()));
        } else {
            close();            // after playing or showing, the window is not needed any more
        }
    }

    private void publishAdd(AddToPlaylistEvent.AddToPlaylistEventBuilder builder) {
        eventPublisher.publishEvent(builder.source(this)
                                           .build());
    }

    private TrackInfo findTrack(SearchResult result) {
        if (result.albumId() == null) {
            return null;
        }
        AlbumRecord album = dataAccess.getAlbum(result.albumId());
        return dataAccess.getAlbumsTracks(List.of(album))
                         .stream()
                         .filter(track -> track.getTrackId() == result.id())
                         .findFirst()
                         .orElse(null);
    }

    private void close() {
        ((Stage) queryField.getScene()
                           .getWindow()).close();
    }

    /**
     * An icon for the kind of hit, the name, and what it belongs to.
     */
    private static class ResultCell extends ListCell<SearchResult> {
        @Override
        protected void updateItem(SearchResult result, boolean empty) {
            super.updateItem(result, empty);
            if (empty || result == null) {
                setGraphic(null);
                return;
            }
            String icon = switch (result.kind()) {
                case ARTIST -> "mdi2a-account-music";
                case ALBUM -> "mdi2a-album";
                case TRACK -> UiIcons.MUSIC_NOTE;
            };
            Label title = new Label(result.title());
            title.getStyleClass()
                 .add("text-strong");
            Label subtitle = new Label(result.subtitle());
            subtitle.getStyleClass()
                    .add("dim-text");
            HBox row = new HBox(10, UiIcons.icon(icon, 18), title, subtitle);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            HBox.setHgrow(subtitle, Priority.ALWAYS);
            setGraphic(row);
            setAccessibleText(result.title() + ", " + result.subtitle());
        }
    }
}
