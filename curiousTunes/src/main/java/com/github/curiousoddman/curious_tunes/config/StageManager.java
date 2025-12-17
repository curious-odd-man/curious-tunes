package com.github.curiousoddman.curious_tunes.config;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.Objects;

@RequiredArgsConstructor
public class StageManager {
    private final FxmlLoader fxmlLoader;
    private final Stage primaryStage;
    private final String applicationTitle;
    private final ApplicationEventPublisher eventPublisher;

    public void switchScene(final FxmlView view) {
        primaryStage.setMinWidth(1010);
        primaryStage.setMinHeight(700);
        primaryStage.setTitle(applicationTitle);

        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        Parent rootNode = loadRootNode(view.getFxmlPath());


        Scene scene = new Scene(rootNode);
/*        String stylesheet = Objects.requireNonNull(getClass()
                        .getResource("/styles/styles.css"))
                .toExternalForm();

        scene.getStylesheets().add(stylesheet);*/

        scene.widthProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldSceneWidth,
                    Number newSceneWidth) {

                //eventPublisher.publishEvent(new SceneResizeEvent(this, newSceneWidth));
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void switchToNextScene(final FxmlView view) {

        Parent rootNode = loadRootNode(view.getFxmlPath());
        primaryStage.getScene().setRoot(rootNode);

        primaryStage.show();
    }


    private Parent loadRootNode(String fxmlPath) {
        Parent rootNode;
        try {
            rootNode = fxmlLoader.load(fxmlPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rootNode;
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
