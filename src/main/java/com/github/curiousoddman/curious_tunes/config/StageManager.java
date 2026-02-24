package com.github.curiousoddman.curious_tunes.config;

import com.github.curiousoddman.curious_tunes.event.UserShutdownApplication;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
public class StageManager {
    private final FxmlLoader fxmlLoader;
    private final Stage primaryStage;
    private final String applicationTitle;
    private final ApplicationEventPublisher eventPublisher;

    public StageManager(FxmlLoader fxmlLoader,
                        Stage primaryStage,
                        String applicationTitle,
                        ApplicationEventPublisher eventPublisher) {
        this.fxmlLoader = fxmlLoader;
        this.primaryStage = primaryStage;
        this.applicationTitle = applicationTitle;
        this.eventPublisher = eventPublisher;

        primaryStage.setOnCloseRequest(event -> {
            log.info("User requested application shutdown");
            eventPublisher.publishEvent(new UserShutdownApplication(this));
        });
    }

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
