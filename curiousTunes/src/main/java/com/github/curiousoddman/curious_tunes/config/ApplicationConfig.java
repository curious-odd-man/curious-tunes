package com.github.curiousoddman.curious_tunes.config;

import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final FxmlLoader fxmlLoader;
    @Value("${application.title}")
    private final String applicationTitle;
    private final ApplicationEventPublisher eventPublisher;

    @Bean
    @Lazy
    public StageManager stageManager(Stage stage) {
        return new StageManager(fxmlLoader, stage, applicationTitle, eventPublisher);
    }
}
