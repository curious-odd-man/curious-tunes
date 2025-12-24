package com.github.curiousoddman.curious_tunes.config;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
public class StageManager {
    private final FxmlLoader fxmlLoader;
    private final Stage primaryStage;
    private final String applicationTitle;
    private final ApplicationEventPublisher eventPublisher;

    public void switchScene(FxmlView view) {
        primaryStage.setTitle(applicationTitle);
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        Parent rootNode = loadRootNode(view);
        Scene scene = new Scene(rootNode);

        scene.widthProperty().addListener((observableValue, oldSceneWidth, newSceneWidth) -> {

            //eventPublisher.publishEvent(new SceneResizeEvent(this, newSceneWidth));
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Parent loadRootNode(FxmlView fxmlPath) {
        return fxmlLoader.load(fxmlPath, null).parent();
    }

    public void switchToFullScreenMode() {
        primaryStage.setFullScreen(true);
    }

    public void switchToWindowedMode() {
        primaryStage.setFullScreen(false);
    }

    public boolean isStageFullScreen() {
        return primaryStage.isFullScreen();
    }

    public void exit() {
        primaryStage.close();
    }

}
