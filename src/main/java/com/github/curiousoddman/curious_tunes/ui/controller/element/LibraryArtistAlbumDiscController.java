package com.github.curiousoddman.curious_tunes.ui.controller.element;

import com.github.curiousoddman.curious_tunes.config.FxmlLoader;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumDiscBundle;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistAlbumTrackBundle;
import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import static com.github.curiousoddman.curious_tunes.util.styles.CssClasses.DISC_NAME;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistAlbumDiscController implements Initializable {
    private final FxmlLoader fxmlLoader;
    public VBox pane;
    public Label diskNumberLabel;
    public VBox tracksLeftColumnVbox;
    public VBox tracksRightColumnVbox;

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        diskNumberLabel.getStyleClass().add(DISC_NAME);
        if (resources instanceof ArtistAlbumDiscBundle albumDiscBundle) {
            log.info("Album {} disk {}", albumDiscBundle.getAlbumInfo().getName(), albumDiscBundle.getDiscNumber());
            if (albumDiscBundle.getDiscNumber() != null) {
                diskNumberLabel.setText("💿 " + albumDiscBundle.getDiscNumber());
            } else {
                pane.getChildren().remove(diskNumberLabel);
            }

            List<TrackRecord> discTracks = albumDiscBundle.getTrackRecords();
            int tracksPerColumn = discTracks.size() <= 10
                    ? discTracks.size()
                    : (discTracks.size() / 2);

            AlbumInfo albumInfo = albumDiscBundle.getAlbumInfo();
            Iterator<TrackRecord> iterator = discTracks.iterator();
            int row = 0;
            VBox col = tracksLeftColumnVbox;
            while (iterator.hasNext()) {
                TrackRecord trackRecord = iterator.next();
                LoadedFxml<LibraryArtistAlbumTrackController> loadedFxml = fxmlLoader.load(
                        FxmlView.LIBRARY_ALBUM_TRACK,
                        new ArtistAlbumTrackBundle(
                                albumInfo.toTrackInfo(trackRecord),
                                albumDiscBundle.getTrackSelectionModel()
                        )
                );
                Parent parent = loadedFxml.parent();
                col.getChildren().add(parent);
                if (row + 1 == tracksPerColumn) {
                    col = tracksRightColumnVbox;
                    row = 0;
                } else {
                    row++;
                }
            }
        }
    }
}
