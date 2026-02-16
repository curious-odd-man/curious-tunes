package com.github.curiousoddman.curious_tunes.app.preloader;

import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AnimatedPreloader extends Preloader {
    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;

        Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/preloader.fxml")
        );

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof MainSceneVisible) {
            stage.hide();
        }
    }
}