package com.github.curiousoddman.curious_tunes.app.preloader;

import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class AnimatedPreloader extends Preloader {
    private Stage               stage;
    private PreloaderController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        stage.getIcons()
             .addAll(
                     new Image(getClass().getResourceAsStream("/icons/app-icon-16.png")),
                     new Image(getClass().getResourceAsStream("/icons/app-icon-32.png")),
                     new Image(getClass().getResourceAsStream("/icons/app-icon-64.png")),
                     new Image(getClass().getResourceAsStream("/icons/app-icon-128.png"))
             );

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/preloader.fxml"));
        Parent     root   = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof MainSceneVisible) {
            stage.hide();
        } else if (info instanceof PreloaderStatus status && controller != null) {
            controller.setStatus(status.message());
        }
    }
}