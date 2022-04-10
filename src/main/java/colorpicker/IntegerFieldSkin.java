package colorpicker;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;

public class IntegerFieldSkin extends InputFieldSkin {
    private final InvalidationListener integerFieldValueListener;

    public IntegerFieldSkin(IntegerField control) {
        super(control);
        control.valueProperty().addListener(integerFieldValueListener = (observable -> updateText()));
    }

    @Override
    public IntegerField getSkinnable() {
        return (IntegerField) control;
    }

    @Override
    public Node getNode() {
        return getTextField();
    }

    @Override
    public void dispose() {
        ((IntegerField) control).valueProperty().removeListener(integerFieldValueListener);
        super.dispose();
    }

    @Override
    protected boolean accept(String text) {
        if (text.length() == 0)
            return true;
        if (text.matches("[0-9]*"))
            try {
                Integer.parseInt(text);
                int value = Integer.parseInt(text);
                int maxValue = ((IntegerField) control).getMaxValue();
                return maxValue == -1 || (value <= maxValue);
            } catch (NumberFormatException numberFormatException) {
            }
        return false;
    }

    @Override
    protected void updateText() {
        getTextField().setText(String.valueOf(((IntegerField) control).getValue()));
    }

    @Override
    protected void updateValue() {
        int value = ((IntegerField) control).getValue();
        String text = (getTextField().getText() == null) ? "" : getTextField().getText().trim();
        try {
            int newValue = Integer.parseInt(text);
            if (newValue != value)
                ((IntegerField) control).setValue(newValue);
        } catch (NumberFormatException ex) {
            ((IntegerField) control).setValue(0);
            Platform.runLater(() -> getTextField().positionCaret(1));
        }
    }
}
