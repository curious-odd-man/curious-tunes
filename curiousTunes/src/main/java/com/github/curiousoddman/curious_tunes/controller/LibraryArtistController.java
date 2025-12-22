package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.event.ShowArtistAlbums;
import com.github.curiousoddman.curious_tunes.model.ArtistSelectionModel;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistItemBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Lazy
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class LibraryArtistController implements Initializable {
    private final ApplicationEventPublisher eventPublisher;

    public ImageView artistImageView;
    public Label artistNameLabel;

    private ArtistRecord artistRecord;
    private ArtistSelectionModel artistSelectionModel;


    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        if (resources instanceof ArtistItemBundle bundle) {
            ArtistRecord artist = bundle.getArtist();
            //String icon = bundle.getIcon(); // FIXME:
            //artistImageView.setImage(new Image(Path.of(icon).toUri().toString()));
            artistNameLabel.setText(artist.getName());
            this.artistRecord = artist;
            this.artistSelectionModel = bundle.getArtistSelectionModel();
        }
    }

    public void onArtistClicked(MouseEvent mouseEvent) {
        artistSelectionModel.getOptionalSelectedItem().ifPresent(LibraryArtistController::clearSelection);
        artistSelectionModel.select(this);
        artistNameLabel.getParent().setStyle("-fx-background-color: #00F;");
        artistNameLabel.setStyle("-fx-text-fill: #FFF");
        eventPublisher.publishEvent(new ShowArtistAlbums(this, artistRecord));
    }

    public void clearSelection() {
        artistNameLabel.getParent().setStyle("");
        artistNameLabel.setStyle("");
    }

    @FXML
    public void onPlayClicked(ActionEvent actionEvent) {
        //TODO:
    }

    @FXML
    public void onAddPlayClicked(ActionEvent actionEvent) {
//TODO:
    }

    @FXML
    public void onShuffleClicked(ActionEvent actionEvent) {
//TODO:
    }

    @FXML
    public void onAddShuffledClicked(ActionEvent actionEvent) {
//TODO:
    }
}
