package com.github.curiousoddman.curious_tunes.app.preloader;

import com.github.curiousoddman.curious_tunes.util.AppResources;
import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AnimatedPreloader extends Preloader {
    private Stage               stage;
    private PreloaderController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        stage.getIcons()
             .addAll(AppResources.appIcons());

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