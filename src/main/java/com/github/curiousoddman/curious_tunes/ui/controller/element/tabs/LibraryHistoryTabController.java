package com.github.curiousoddman.curious_tunes.ui.controller.element.tabs;

import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.event.UserNotificationEvent;
import com.github.curiousoddman.curious_tunes.model.HistoryRow;
import com.github.curiousoddman.curious_tunes.model.HistoryStats;
import com.github.curiousoddman.curious_tunes.util.HistoryFormat;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What was played: totals for a period and the latest plays (loaded in pages, off the UI thread).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryHistoryTabController implements Initializable {
    private static final int PAGE_SIZE = 200;

    private final DataAccess                dataAccess;
    private final ApplicationEventPublisher eventPublisher;

    @FXML
    public ToggleButton                            todayToggle;
    @FXML
    public ToggleButton                            weekToggle;
    @FXML
    public ToggleButton                            monthToggle;
    @FXML
    public ToggleButton                            allToggle;
    @FXML
    public Label                                   totalTimeListened;
    @FXML
    public Label                                   songsPlayed;
    @FXML
    public Label                                   albumsPlayed;
    @FXML
    public Label                                   artistsPlayed;
    @FXML
    public TableView<HistoryRow>                   latestSongsTable;
    @FXML
    public TableColumn<HistoryRow, OffsetDateTime> playedAtColumn;
    @FXML
    public TableColumn<HistoryRow, String>         songColumn;
    @FXML
    public TableColumn<HistoryRow, String>         albumColumn;
    @FXML
    public TableColumn<HistoryRow, String>         artistColumn;
    @FXML
    public Label                                   emptyLabel;
    @FXML
    public ProgressIndicator                       loadingIndicator;
    @FXML
    public Label                                   shownLabel;
    @FXML
    public Button                                  loadMoreButton;

    private final ObservableList<HistoryRow> rows    = FXCollections.observableArrayList();
    private final AtomicInteger              loadId  = new AtomicInteger();
    private final NumberFormat               numbers = NumberFormat.getIntegerInstance();

    private OffsetDateTime since;       // start of the shown period; null = everything
    private long           totalPlays;
    private boolean        loading;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        playedAtColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()
                                                                                   .playedAt()));
        playedAtColumn.setCellFactory(_ -> new PlayedAtCell());
        playedAtColumn.setSortType(TableColumn.SortType.DESCENDING);
        songColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()
                                                                             .title()));
        albumColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()
                                                                              .album()));
        artistColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()
                                                                               .artist()));
        latestSongsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        latestSongsTable.setPlaceholder(new Label(""));         // the empty state is shown by emptyLabel
        latestSongsTable.setItems(rows);
        latestSongsTable.getSortOrder()
                        .add(playedAtColumn);

        ToggleGroup period = new ToggleGroup();
        for (ToggleButton toggle : List.of(todayToggle, weekToggle, monthToggle, allToggle)) {
            toggle.setToggleGroup(period);
        }
        period.selectedToggleProperty()
              .addListener((obs, previous, current) -> {
                  if (current == null) {
                      previous.setSelected(true);         // one period is always chosen
                      return;
                  }
                  reload();
              });
    }

    /**
     * Loads the totals and the first page again (called each time the tab is shown).
     */
    public void renewStats() {
        reload();
    }

    @FXML
    public void onLoadMore(ActionEvent actionEvent) {
        if (loading) {
            return;
        }
        int            id     = loadId.get();
        int            offset = rows.size();
        OffsetDateTime period = since;
        setLoading(true);
        runInBackground(id, () -> dataAccess.getHistoryRows(period, PAGE_SIZE, offset), page -> {
            rows.addAll(page);
            setLoading(false);
            refreshFooter();
        });
    }

    private void reload() {
        int            id     = loadId.incrementAndGet();
        OffsetDateTime period = sinceOf(selectedPeriod());
        since = period;
        setLoading(true);
        runInBackground(id, () -> new Loaded(dataAccess.getHistoryStats(period), dataAccess.getHistoryRows(period, PAGE_SIZE, 0)),
                loaded -> {
                    showStats(loaded.stats());
                    totalPlays = loaded.stats()
                                       .plays();
                    rows.setAll(loaded.rows());
                    setLoading(false);
                    refreshFooter();
                });
    }

    private record Loaded(HistoryStats stats, List<HistoryRow> rows) {
    }

    /**
     * Runs the query in a thread of its own; the result is shown only if no newer load was started meanwhile.
     */
    private <T> void runInBackground(int id, java.util.function.Supplier<T> query, java.util.function.Consumer<T> show) {
        Thread t = new Thread(() -> {
            try {
                T result = query.get();
                Platform.runLater(() -> {
                    if (id == loadId.get()) {
                        show.accept(result);
                    }
                });
            } catch (Exception e) {
                log.error("Unable to load the history", e);
                Platform.runLater(() -> {
                    if (id == loadId.get()) {
                        setLoading(false);
                        refreshFooter();
                        eventPublisher.publishEvent(UserNotificationEvent.error(this, "Unable to load the history"));
                    }
                });
            }
        }, "Load history");
        t.setDaemon(true);
        t.start();
    }

    private Toggle selectedPeriod() {
        Toggle selected = todayToggle.getToggleGroup()
                                     .getSelectedToggle();
        return selected == null ? allToggle : selected;
    }

    private OffsetDateTime sinceOf(Toggle toggle) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        if (toggle == todayToggle) {
            return now.toLocalDate()
                      .atStartOfDay(now.getZone())
                      .toOffsetDateTime();
        }
        if (toggle == weekToggle) {
            return now.minusDays(7)
                      .toOffsetDateTime();
        }
        if (toggle == monthToggle) {
            return now.minusDays(30)
                      .toOffsetDateTime();
        }
        return null;
    }

    private void showStats(HistoryStats stats) {
        totalTimeListened.setText(stats.plays() == 0 ? "–" : HistoryFormat.duration(stats.totalSeconds()));
        songsPlayed.setText(numbers.format(stats.plays()));
        albumsPlayed.setText(numbers.format(stats.albums()));
        artistsPlayed.setText(numbers.format(stats.artists()));
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        setShown(loadingIndicator, loading);
        loadMoreButton.setDisable(loading);
        refreshEmptyState();
    }

    private void refreshEmptyState() {
        boolean empty = rows.isEmpty() && !loading;
        emptyLabel.setText(since == null ? "Nothing played yet" : "Nothing played in this period");
        setShown(emptyLabel, empty);
    }

    private void refreshFooter() {
        refreshEmptyState();
        shownLabel.setText(rows.isEmpty() ? "" : "Showing " + numbers.format(rows.size()) + " of " + numbers.format(totalPlays) + " plays");
        setShown(loadMoreButton, rows.size() < totalPlays);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /**
     * "Today 14:03" in the cell; the complete date and time in a tooltip.
     */
    private static class PlayedAtCell extends TableCell<HistoryRow, OffsetDateTime> {
        @Override
        protected void updateItem(OffsetDateTime playedAt, boolean empty) {
            super.updateItem(playedAt, empty);
            if (empty || playedAt == null) {
                setText(null);
                setTooltip(null);
                return;
            }
            ZoneId zone   = ZoneId.systemDefault();
            Locale locale = Locale.getDefault();
            setText(HistoryFormat.playedAt(playedAt, Instant.now(), zone, locale));
            setTooltip(new Tooltip(HistoryFormat.full(playedAt, zone, locale)));
        }
    }
}
