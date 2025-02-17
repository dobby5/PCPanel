package com.getpcpanel.ui;

import java.util.Collection;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.getpcpanel.cpp.AudioDevice;
import com.getpcpanel.cpp.ISndCtrl;
import com.getpcpanel.device.Device;
import com.getpcpanel.profile.LightingConfig;
import com.getpcpanel.profile.LightingConfig.LightingMode;
import com.getpcpanel.profile.SaveService;
import com.getpcpanel.profile.SingleKnobLightingConfig.SINGLE_KNOB_MODE;
import com.getpcpanel.spring.Prototype;
import com.getpcpanel.ui.colorpicker.ColorDialog;
import com.getpcpanel.ui.colorpicker.HueSlider;
import com.getpcpanel.util.Util;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@Prototype
@RequiredArgsConstructor
public class MiniLightingDialog extends Application implements UIInitializer, ILightingDialogMuteOverrideHelper {
    private final SaveService saveService;
    private final ApplicationEventPublisher eventPublisher;
    private final ISndCtrl sndCtrl;
    private Device device;

    private Stage stage;
    @FXML private TabPane mainPane;
    @FXML private TabPane knobsTabbedPane;
    @FXML private TabPane fullBodyTabbedPane;
    @FXML private Slider rainbowPhaseShift;
    @FXML private Slider rainbowBrightness;
    @FXML private Slider rainbowSpeed;
    @FXML private CheckBox rainbowReverse;
    @FXML private CheckBox rainbowVertical;
    private HueSlider waveHue;
    @FXML private Slider waveBrightness;
    @FXML private Slider waveSpeed;
    @FXML private CheckBox waveReverse;
    @FXML private CheckBox waveBounce;
    private HueSlider breathHue;
    @FXML private Slider breathBrightness;
    @FXML private Slider breathSpeed;
    @FXML private VBox wavebox;
    @FXML private VBox breathbox;
    @FXML private Button applyToAllButton;
    private ColorDialog allKnobColor;
    private static final int NUM_KNOBS = 4;
    private final TabPane[] knobSingleTabPane = new TabPane[NUM_KNOBS];
    private final ColorDialog[] knobStaticCDs = new ColorDialog[NUM_KNOBS];
    private final ColorDialog[] knobVolumeGradientCD1 = new ColorDialog[NUM_KNOBS];
    private final ColorDialog[] knobVolumeGradientCD2 = new ColorDialog[NUM_KNOBS];
    @Getter private final CheckBox[] muteOverrideCheckboxesKnobs = new CheckBox[NUM_KNOBS];
    @SuppressWarnings("unchecked") @Getter private final ComboBox<String>[] muteOverrideComboBoxesKnobs = new ComboBox[NUM_KNOBS];
    @Getter private final ColorDialog[] muteOverrideColorsKnobs = new ColorDialog[NUM_KNOBS];

    private boolean pressedOk;
    @FXML private Pane root;
    private LightingConfig lightingConfig;

    @Override
    public <T> void initUI(T... args) {
        device = getUIArg(Device.class, args, 0);
        lightingConfig = device.getSavedLightingConfig().deepCopy();
        setDeviceLighting();
        postInit();
    }

    private void setDeviceLighting() {
        device.setLighting(lightingConfig.deepCopy(), true);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        var scene = new Scene(Objects.requireNonNull(root));
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/assets/dark_theme.css")).toExternalForm());
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResource("/assets/256x256.png")).toExternalForm()));
        stage.setOnHiding(e -> {
            if (!pressedOk) {
                device.setLighting(device.getSavedLightingConfig(), true);
            }
            eventPublisher.publishEvent(LightningChangedToDefaultEvent.INSTANCE);
        });
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(HomePage.stage);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setTitle("Lighting Dialog");
        stage.show();
    }

    public MiniLightingDialog select(int idx) {
        mainPane.getSelectionModel().select(1);
        knobsTabbedPane.getSelectionModel().select(idx);
        return this;
    }

    @FXML
    private void onCancel(ActionEvent event) {
        stage.close();
    }

    @FXML
    private void ok(ActionEvent event) {
        log.debug("{} {}", stage.getWidth(), stage.getHeight());
        pressedOk = true;
        device.setSavedLighting(lightingConfig);
        saveService.save();
        stage.close();
    }

    @FXML
    private void turnOffLights(ActionEvent event) {
        allKnobColor.setCustomColor(Color.WHITE);
        allKnobColor.setCustomColor(Color.BLACK);
        mainPane.getSelectionModel().select(0);
        fullBodyTabbedPane.getSelectionModel().select(0);
    }

    private void postInit() {
        for (var i = 0; i < NUM_KNOBS; i++) {
            var knob = i + 1;
            var tab = new Tab("Knob " + knob);
            var cd = new ColorDialog(Color.BLACK);
            knobStaticCDs[i] = cd;
            knobVolumeGradientCD1[i] = new ColorDialog();
            knobVolumeGradientCD2[i] = new ColorDialog();
            var volGradientGP = makeFourPanelGridPane(
                    knobVolumeGradientCD2[i], knobVolumeGradientCD1[i]);
            var vbox = new VBox(volGradientGP);
            var staticTab = new Tab("Static", cd);
            var volGradient = new Tab("Volume Gradient", vbox);
            var singleKnobTabPane = new TabPane(staticTab, volGradient);
            knobSingleTabPane[i] = singleKnobTabPane;
            Util.adjustTabs(singleKnobTabPane, 140, 30);
            singleKnobTabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
            singleKnobTabPane.setSide(Side.LEFT);
            tab.setContent(tabWithMuteOverride(OverrideTargetType.KNOB, i, singleKnobTabPane));
            knobsTabbedPane.getTabs().add(tab);
        }
        Util.adjustTabs(fullBodyTabbedPane, 120, 30);
        allKnobColor = new ColorDialog();
        fullBodyTabbedPane.getTabs().get(0).setContent(allKnobColor);
        var allSliders = new Slider[] { rainbowPhaseShift, rainbowBrightness, rainbowSpeed,
                waveBrightness, waveSpeed,
                breathBrightness, breathSpeed };
        var allCheckBoxes = new CheckBox[] { rainbowReverse, rainbowVertical,
                waveReverse, waveBounce };
        waveHue = new HueSlider();
        wavebox.getChildren().add(1, waveHue);
        breathHue = new HueSlider();
        breathbox.getChildren().add(1, breathHue);
        applyToAllButton.setOnAction(e -> {
            if (mainPane.getSelectionModel().getSelectedIndex() == 1) {
                var knobIndex = knobsTabbedPane.getSelectionModel().getSelectedIndex();
                for (var i = 0; i < lightingConfig.getKnobConfigs().length; i++) {
                    if (i != knobIndex)
                        lightingConfig.getKnobConfigs()[i].set(lightingConfig.getKnobConfigs()[knobIndex]);
                }
            }
            initFields();
        });
        initFields();
        initListeners(allSliders, allCheckBoxes);
    }

    private void initFields() {
        var mode = lightingConfig.getLightingMode();
        if (mode == LightingMode.ALL_COLOR) {
            mainPane.getSelectionModel().select(0);
            fullBodyTabbedPane.getSelectionModel().select(0);
            allKnobColor.setCustomColor(Color.web(lightingConfig.getAllColor()));
        } else if (mode == LightingMode.ALL_RAINBOW) {
            mainPane.getSelectionModel().select(0);
            fullBodyTabbedPane.getSelectionModel().select(1);
            rainbowPhaseShift.setValue(lightingConfig.getRainbowPhaseShift() & 0xFF);
            rainbowBrightness.setValue(lightingConfig.getRainbowBrightness() & 0xFF);
            rainbowSpeed.setValue(lightingConfig.getRainbowSpeed() & 0xFF);
            rainbowReverse.setSelected(lightingConfig.getRainbowReverse() == 1);
            rainbowVertical.setSelected(lightingConfig.getRainbowVertical() == 1);
        } else if (mode == LightingMode.ALL_WAVE) {
            mainPane.getSelectionModel().select(0);
            fullBodyTabbedPane.getSelectionModel().select(2);
            waveHue.setHue(lightingConfig.getWaveHue() & 0xFF);
            waveBrightness.setValue(lightingConfig.getWaveBrightness() & 0xFF);
            waveSpeed.setValue(lightingConfig.getWaveSpeed() & 0xFF);
            waveReverse.setSelected(lightingConfig.getWaveReverse() == 1);
            waveBounce.setSelected(lightingConfig.getWaveBounce() == 1);
        } else if (mode == LightingMode.ALL_BREATH) {
            mainPane.getSelectionModel().select(0);
            fullBodyTabbedPane.getSelectionModel().select(3);
            breathHue.setHue(lightingConfig.getBreathHue() & 0xFF);
            breathBrightness.setValue(lightingConfig.getBreathBrightness() & 0xFF);
            breathSpeed.setValue(lightingConfig.getBreathSpeed() & 0xFF);
        } else if (mode == LightingMode.CUSTOM) {
            if (mainPane.getSelectionModel().getSelectedIndex() == 0)
                mainPane.getSelectionModel().select(1);
            var knobConfigs = lightingConfig.getKnobConfigs();
            for (var i = 0; i < NUM_KNOBS; i++) {
                var knobConfig = knobConfigs[i];
                if (knobConfig.getMode() == SINGLE_KNOB_MODE.STATIC) {
                    knobSingleTabPane[i].getSelectionModel().select(0);
                    knobStaticCDs[i].setCustomColor(Color.web(knobConfig.getColor1()));
                } else if (knobConfig.getMode() == SINGLE_KNOB_MODE.VOLUME_GRADIENT) {
                    knobSingleTabPane[i].getSelectionModel().select(1);
                    knobVolumeGradientCD1[i].setCustomColor(Color.web(knobConfig.getColor1()));
                    knobVolumeGradientCD2[i].setCustomColor(Color.web(knobConfig.getColor2()));
                }
                setOverride(OverrideTargetType.KNOB, i, knobConfig.getMuteOverrideDeviceOrFollow(), knobConfig.getMuteOverrideColor());
            }
        }
        updateApplyToAllButton();
    }

    private void addListener(ColorDialog[]... xs) {
        for (var x : xs) {
            for (var cd : x) {
                cd.customColorProperty().addListener((a, bb, c) -> updateColors());
            }
        }
    }

    private void addListener(TabPane... tbs) {
        for (var tb : tbs) {
            tb.getSelectionModel().selectedItemProperty().addListener((a, bb, c) -> updateColors());
        }
    }

    private void addListener(CheckBox[]... checkss) {
        for (var checks : checkss) {
            for (var check : checks) {
                check.setOnAction(a -> updateColors());
            }
        }
    }

    private void addListener(ComboBox<?>[]... boxes) {
        for (var checks : boxes) {
            for (var check : checks) {
                check.setOnAction(a -> updateColors());
            }
        }
    }

    private void initListeners(Slider[] allSliders, CheckBox[] allCheckBoxes) {
        addListener(knobStaticCDs, knobVolumeGradientCD1, allOverrideColors(), knobVolumeGradientCD2);
        addListener(knobSingleTabPane);
        allKnobColor.customColorProperty().addListener((observable, oldValue, newValue) -> {
            for (var cd : knobStaticCDs) {
                cd.setCustomColor(newValue);
            }
            updateColors();
        });
        addListener(fullBodyTabbedPane);
        addListener(allOverrideCheckboxes());
        addListener(allOverrideComboBoxes());
        for (var slider : allSliders) {
            slider.valueProperty().addListener((observable, oldValue, newValue) -> updateColors());
        }
        for (var cb : allCheckBoxes) {
            cb.selectedProperty().addListener((observable, oldValue, newValue) -> updateColors());
        }

        waveHue.getHueProperty().addListener((observable, oldValue, newValue) -> updateColors());
        breathHue.getHueProperty().addListener((observable, oldValue, newValue) -> updateColors());
        mainPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateColors();
            updateApplyToAllButton();
        });
    }

    private void updateApplyToAllButton() {
        if (mainPane.getSelectionModel().getSelectedIndex() == 0 || mainPane.getSelectionModel().getSelectedIndex() == 4) {
            applyToAllButton.setVisible(false);
            return;
        }
        applyToAllButton.setVisible(true);
        if (mainPane.getSelectionModel().getSelectedIndex() == 1) {
            applyToAllButton.setText("Apply To All Knobs");
        } else if (mainPane.getSelectionModel().getSelectedIndex() == 2) {
            applyToAllButton.setText("Apply To All Sliders");
        } else if (mainPane.getSelectionModel().getSelectedIndex() == 3) {
            applyToAllButton.setText("Apply To All Slider Labels");
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private void updateColors() {
        if (mainPane.getSelectionModel().getSelectedIndex() == 0) {
            if (fullBodyTabbedPane.getSelectionModel().getSelectedIndex() == 0) {
                lightingConfig = LightingConfig.createAllColor(allKnobColor.getCustomColor());
                setDeviceLighting();
            } else if (fullBodyTabbedPane.getSelectionModel().getSelectedIndex() == 1) {
                lightingConfig = LightingConfig.createRainbowAnimation((byte) (int) rainbowPhaseShift.getValue(), (byte) (int) rainbowBrightness.getValue(),
                        (byte) (int) rainbowSpeed.getValue(), rainbowReverse.isSelected(), rainbowVertical.isSelected());
                setDeviceLighting();
            } else if (fullBodyTabbedPane.getSelectionModel().getSelectedIndex() == 2) {
                lightingConfig = LightingConfig.createWaveAnimation((byte) waveHue.getHue(), (byte) (int) waveBrightness.getValue(), (byte) (int) waveSpeed.getValue(),
                        waveReverse.isSelected(), waveBounce.isSelected());
                setDeviceLighting();
            } else if (fullBodyTabbedPane.getSelectionModel().getSelectedIndex() == 3) {
                lightingConfig = LightingConfig.createBreathAnimation((byte) breathHue.getHue(), (byte) (int) breathBrightness.getValue(), (byte) (int) breathSpeed.getValue());
                setDeviceLighting();
            }
        } else {
            lightingConfig = new LightingConfig(NUM_KNOBS, 0);
            lightingConfig.setLightingMode(LightingMode.CUSTOM);
            for (var knob = 0; knob < NUM_KNOBS; knob++) {
                var knobConfig = lightingConfig.getKnobConfigs()[knob];
                if (knobSingleTabPane[knob].getSelectionModel().getSelectedIndex() == 0) {
                    knobConfig.setMode(SINGLE_KNOB_MODE.STATIC);
                    knobConfig.setColor1FromColor(knobStaticCDs[knob].getCustomColor());
                } else if (knobSingleTabPane[knob].getSelectionModel().getSelectedIndex() == 1) {
                    knobConfig.setMode(SINGLE_KNOB_MODE.VOLUME_GRADIENT);
                    knobConfig.setColor1FromColor(knobVolumeGradientCD1[knob].getCustomColor());
                    knobConfig.setColor2FromColor(knobVolumeGradientCD2[knob].getCustomColor());
                }
                setOverrideSetting(OverrideTargetType.KNOB, knob, knobConfig::setMuteOverrideDeviceOrFollow, knobConfig::setMuteOverrideColorFromColor);
            }
            setDeviceLighting();
        }
    }

    private static GridPane makeFourPanelGridPane(Node obj1, Node obj2) {
        var gp = new GridPane();
        var l1 = new Label("Color when volume is 100");
        var l2 = new Label("Color when volume is 0");
        l1.setWrapText(true);
        l2.setWrapText(true);
        gp.addColumn(0, l1, l2);
        gp.addColumn(1, obj1, obj2);
        return gp;
    }

    @Override
    public Collection<AudioDevice> getDevices() {
        return sndCtrl.getDevices();
    }
}
