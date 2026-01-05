package com.github.curiousoddman.curious_tunes.config;

import com.github.curiousoddman.curious_tunes.model.LoadedFxml;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class FxmlLoader {
    private final ApplicationContext context;

    @SneakyThrows
    public <T> LoadedFxml<T> load(FxmlView fxmlPath, ResourceBundle resourceBundle) {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(context::getBean);
        URL resource = getClass().getClassLoader().getResource(fxmlPath.getFxmlPath());
        loader.setLocation(resource);
        loader.setResources(resourceBundle);
        loader.setClassLoader(context.getClassLoader());
        Parent parent = loader.load();
        return new LoadedFxml<>(
                parent,
                loader.getController()
        );
    }
}
