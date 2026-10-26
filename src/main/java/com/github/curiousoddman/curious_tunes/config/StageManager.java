package com.github.curiousoddman.curious_tunes.config;

import com.github.curiousoddman.curious_tunes.event.UserShutdownApplication;
import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import com.github.curiousoddman.curious_tunes.util.UiStyles;
import com.github.curiousoddman.curious_tunes.util.styles.CssClasses;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
public class StageManager {
    // Below this the three panes of the library window cannot be used.
    private static final double MIN_WIDTH  = 900;
    private static final double MIN_HEIGHT = 560;

    private final FxmlLoader fxmlLoader;
    private final Stage      primaryStage;
    private final String     applicationTitle;

    public StageManager(FxmlLoader fxmlLoader,
                        Stage primaryStage,
                        String applicationTitle,
                        ApplicationEventPublisher eventPublisher) {
        this.fxmlLoader = fxmlLoader;
        this.primaryStage = primaryStage;
        this.applicationTitle = applicationTitle;

        primaryStage.setOnCloseRequest(event -> {
            log.info("User requested application shutdown");
            eventPublisher.publishEvent(new UserShutdownApplication(this));
        });
    }

    public <T> T switchScene(FxmlView<T> view) {
        primaryStage.setTitle(applicationTitle);
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        LoadedFxml<T> loaded = loadRootNode(view);
        Scene         scene  = new Scene(loaded.parent());
        UiStyles.apply(scene);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);

        // Lets the stylesheet draw selections in a muted color while the window is not focused.
        primaryStage.focusedProperty()
                    .addListener((obs, wasFocused, focused) -> {
                        ObservableList<String> rootStyleClasses = scene.getRoot()
                                                                       .getStyleClass();
                        rootStyleClasses.remove(CssClasses.WINDOW_INACTIVE);
                        if (!focused) {
                            rootStyleClasses.add(CssClasses.WINDOW_INACTIVE);
                        }
                    });

        primaryStage.setScene(scene);
        primaryStage.show();
        return loaded.controller();
    }

    private <T> LoadedFxml<T> loadRootNode(FxmlView<T> fxmlPath) {
        return fxmlLoader.load(fxmlPath, null);
    }
}
