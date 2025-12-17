package com.github.curiousoddman.curious_tunes.config;

public enum FxmlView {
    HOME {
        @Override
        public String getFxmlPath() {
            return "/fxml/home.fxml";
        }
    };

    public abstract String getFxmlPath();
}
