package com.github.curiousoddman.curious_tunes.config;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StageManager {
    private final FxmlLoader fxmlLoader;
    private final Stage primaryStage;
    private final String applicationTitle;

    public void switchScene(FxmlView<?> view) {
        primaryStage.setTitle(applicationTitle);
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        Parent rootNode = loadRootNode(view);
        Scene scene = new Scene(rootNode);
        scene.getStylesheets().add("styles/global.css");

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Parent loadRootNode(FxmlView<?> fxmlPath) {
        return fxmlLoader.load(fxmlPath, null).parent();
    }
}
