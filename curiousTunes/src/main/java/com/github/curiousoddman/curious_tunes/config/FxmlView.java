package com.github.curiousoddman.curious_tunes.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FxmlView {
    LIBRARY(".\\fxml\\library.fxml");

    private final String fxmlPath;
}
