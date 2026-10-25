package com.github.curiousoddman.curious_tunes.ui.controller.custom;

import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import com.github.curiousoddman.curious_tunes.util.ImageUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * One album in the grid view: cover, name and year. A click opens the album, a double click plays it.
 */
public class AlbumCard extends VBox {
    private final ImageView cover = new ImageView();
    private final Label     title = new Label();
    private final Label     year  = new Label();

    public AlbumCard(AlbumInfo albumInfo, double size, Runnable onOpen, Runnable onPlay) {
        getStyleClass().add("album-card");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(4);

        cover.setPreserveRatio(true);
        cover.setImage(new Image(Objects.requireNonNull(AlbumCard.class.getResource("/img/noimage.png")).toExternalForm()));
        ImageUtils.setImageIfPresent(albumInfo, cover);

        title.setText(albumInfo.getName());
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        title.getStyleClass().add("text-body");
        year.setText(albumInfo.getAlbumYear() == null ? "" : String.valueOf(albumInfo.getAlbumYear()));
        year.getStyleClass().addAll("text-small", "dim-text");
        Tooltip.install(this, new Tooltip(albumInfo.getName() + "\nClick to open, double-click to play"));

        getChildren().addAll(cover, title, year);
        setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (event.getClickCount() > 1) {
                onPlay.run();
            } else {
                onOpen.run();
            }
        });
        setCoverSize(size);
    }

    public void setCoverSize(double size) {
        cover.setFitWidth(size);
        cover.setFitHeight(size);
        title.setMaxWidth(size);
        year.setMaxWidth(size);
        setPrefWidth(size);
        setMaxWidth(size);
    }
}
