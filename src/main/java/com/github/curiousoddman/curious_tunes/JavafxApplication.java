package com.github.curiousoddman.curious_tunes;

import com.github.curiousoddman.curious_tunes.util.AppResources;
import com.github.curiousoddman.curious_tunes.app.preloader.MainSceneVisible;
import com.github.curiousoddman.curious_tunes.app.preloader.PreloaderStatus;
import com.github.curiousoddman.curious_tunes.config.FxmlView;
import com.github.curiousoddman.curious_tunes.config.StageManager;
import com.github.curiousoddman.curious_tunes.ui.controller.screen.LibraryController;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;


public class JavafxApplication extends Application {
    private Stage stage;

    private ConfigurableApplicationContext applicationContext;
    private StageManager                   stageManager;

    @Override
    public void init() {
        notifyPreloader(new PreloaderStatus("Starting the application..."));
        applicationContext = new SpringApplicationBuilder(Main.class).run();
        notifyPreloader(new PreloaderStatus("Opening the library..."));
    }

    @Override
    public void stop() {
        applicationContext.close();
        stage.close();
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.getIcons()
             .addAll(AppResources.appIcons());
        stageManager = applicationContext.getBean(StageManager.class, primaryStage);
        LibraryController libraryController = stageManager.switchScene(FxmlView.LIBRARY);
        libraryController.setUserPrefs(primaryStage);
        libraryController.installShortcuts(primaryStage.getScene());
        notifyPreloader(new MainSceneVisible());
    }
}
