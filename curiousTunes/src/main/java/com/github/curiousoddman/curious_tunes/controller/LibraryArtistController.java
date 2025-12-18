package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.event.ShowArtistAlbums;
import com.github.curiousoddman.curious_tunes.model.bundle.ArtistItemBundle;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Path;
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

    @Override
    @SneakyThrows
    public void initialize(URL location, ResourceBundle resources) {
        if (resources instanceof ArtistItemBundle bundle) {
            String artist = bundle.getArtist();
            String icon = bundle.getIcon();
            artistImageView.setImage(new Image(Path.of(icon).toUri().toString()));
            artistNameLabel.setText(artist);
        }
    }

    public void onArtistClicked(MouseEvent mouseEvent) {
        eventPublisher.publishEvent(new ShowArtistAlbums(this, artistNameLabel.getText()));
    }
}
