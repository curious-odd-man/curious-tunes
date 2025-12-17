package com.github.curiousoddman.curious_tunes.controller;

import com.github.curiousoddman.curious_tunes.config.StageManager;
import com.github.curiousoddman.curious_tunes.event.LoginEvent;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Lazy
@Component
@RequiredArgsConstructor
public class HomeController implements Initializable {

    @FXML
    private Button theoryButton;

    @FXML
    private Button generatorButton;

    @FXML
    private Button scalesButton;

    @FXML
    private Button chordsButton;

    @FXML
    private Button intervalsButton;

    @FXML
    private Button dictationsButton;

    @FXML
    private Label helloLabel;

    @FXML
    private ImageView gClef;

    private final StageManager stageManager;

    StringProperty nameProperty = new SimpleStringProperty();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gClef.setFitWidth(1000);
        gClef.setPreserveRatio(true);
        gClef.setSmooth(true);

        helloLabel.textProperty().bind(nameProperty);
    }

    @FXML
    void switchToTheoryScene() {
//        stageManager.switchToNextScene(FxmlView.SCALES_THEORY);
    }

    @FXML
    void switchToScalesScene() {
//        stageManager.switchToNextScene(FxmlView.SCALES);
    }

    @FXML
    void switchToChordsScene() {

    }

    @FXML
    public void switchToChordGeneratorScene() {
    }

    @EventListener
    public void handleLoginEvent(LoginEvent event) {
        nameProperty.setValue("Hello, " + event.getUserName() + "!");
    }

    @FXML
    public void switchToIntervalsScene(ActionEvent actionEvent) {
//        stageManager.switchToNextScene(FxmlView.INTERVALS);
    }

    @FXML
    public void switchToDictationsScene(ActionEvent actionEvent) {

    }
}
