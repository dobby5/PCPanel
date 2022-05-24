package main;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import obs.OBSListener;
import obsremote.OBSRemoteController;
import save.Save;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

@Log4j2
public class SettingsDialog extends Application implements Initializable {

    private Stage stage;

    private Pane pane;

    private final Stage parentStage;

    @FXML
    private CheckBox obsEnable;

    @FXML
    private Pane obsControls;

    @FXML
    private TextField obsAddress;

    @FXML
    private TextField obsPort;

    @FXML
    private TextField obsPassword;

    @FXML
    private Label obsTestResult;

    @FXML
    private Hyperlink obsLink;

    @FXML
    private CheckBox vmEnable;

    @FXML
    private Pane vmControls;

    @FXML
    private TextField vmPath;

    public SettingsDialog(Stage parentStage) {
        this.parentStage = parentStage;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/assets/SettingsDialog.fxml"));
        loader.setController(this);
        try {
            pane = loader.load();
        } catch (IOException e) {
            log.error("Unable to load loader", e);
        }
        Scene scene = new Scene(pane);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/assets/dark_theme.css"), "Unable to find dark_theme.css").toExternalForm());
        stage.getIcons().add(new Image("/assets/256x256.png"));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.setTitle("Settings");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.showAndWait();
    }

    @FXML
    private void onOBSEnablePressed(ActionEvent ignored) {
        obsControls.setDisable(!obsEnable.isSelected());
    }

    @FXML
    private void onVMEnablePressed(ActionEvent ignored) {
        vmControls.setDisable(!vmEnable.isSelected());
    }

    @FXML
    private void obsTest(ActionEvent event) {
        OBSRemoteController controller = new OBSRemoteController(obsAddress.getText(), obsPort.getText(), obsPassword.getText());
        if (controller.isFailed()) {
            obsTestResult.setText("result: connection failed");
        } else {
            obsTestResult.setText("result: success");
        }
        controller.disconnect();
    }

    @FXML
    private void ok(ActionEvent event) {
        Save.setObsEnabled(obsEnable.isSelected());
        Save.setObsAddress(obsAddress.getText());
        Save.setObsPort(obsPort.getText());
        Save.setObsPassword(obsPassword.getText());
        Save.setVoicemeeterEnabled(vmEnable.isSelected());
        Save.setVoicemeeterPath(vmPath.getText());
        Save.saveFile();
        OBSListener.check();
        stage.close();
    }

    @FXML
    private void closeButtonAction(ActionEvent event) {
        stage.close();
    }

    @FXML
    private void openLogsFolder(ActionEvent event) {
        try {
            Runtime.getRuntime().exec("cmd /c \"start logs\"");
        } catch (IOException e) {
            log.error("Unable to open logs folder", e);
        }
    }

    private void initFields() {
        obsEnable.setSelected(Save.isObsEnabled());
        obsAddress.setText(Save.getObsAddress());
        obsPort.setText(Save.getObsPort());
        obsPassword.setText(Save.getObsPassword());
        onOBSEnablePressed(null);
        vmEnable.setSelected(Save.isVoicemeeterEnabled());
        vmPath.setText(Save.getVoicemeeterPath());
        onVMEnablePressed(null);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        obsLink.setOnAction(c -> getHostServices().showDocument(obsLink.getText()));
        initFields();
    }
}
