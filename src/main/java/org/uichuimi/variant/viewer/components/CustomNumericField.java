package org.uichuimi.variant.viewer.components;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.NumberFormat;
import java.text.ParseException;

public class CustomNumericField extends VBox {

	private final Label unitLabel = new Label();
	private final TextField textField = new TextField();
	private final HBox box = new HBox(textField, unitLabel);
	private final Property<BigDecimal> value = new SimpleObjectProperty<>();
	private final Property<Integer> integerValue = new SimpleObjectProperty<>();
	private final Property<Double> doubleValue = new SimpleObjectProperty<>();
	private final NumberFormat format = NumberFormat.getInstance();
	private String promptText;
	private MathContext context = new MathContext(3);

	{
		final ChangeListener<BigDecimal> listener = (obs, prev, val) -> {
			integerValue.setValue(val == null ? null : val.intValue());
			doubleValue.setValue(val == null ? null : val.doubleValue());
			updateView();
		};
		value.addListener(listener);
		integerValue.addListener((obs, prev, val) -> {
			value.removeListener(listener);
			value.setValue(val == null ? null : new BigDecimal(val.toString(), context));
			value.addListener(listener);
			updateView();
		});
		doubleValue.addListener((obs, prev, val) -> {
			value.removeListener(listener);
			value.setValue(val == null ? null : new BigDecimal(val.toString(), context));
			value.addListener(listener);
			updateView();
		});
	}
	public CustomNumericField() {
		this(null);
	}

	public <V extends Number> CustomNumericField(V value) {
		setValue(value);
		getStyleClass().add("custom-numeric-field");
		box.getStyleClass().add("content-box");
		unitLabel.getStyleClass().add("unit-label");
		unitLabel.setMinWidth(Region.USE_PREF_SIZE);
		textField.setPrefWidth(80);
		HBox.setHgrow(unitLabel, Priority.ALWAYS);
		HBox.setHgrow(textField, Priority.SOMETIMES);
		setFillWidth(true);
		box.setFillHeight(true);
		getChildren().addAll(box);
		textField.setOnAction(event -> parseValue());
		updateView();
	}

	public <V extends Number> void setValue(V value) {
		this.value.setValue(value == null ? null : new BigDecimal(value.toString(), context));
		updateView();
	}

	public BigDecimal getValue() {
		return value.getValue();
	}

	public Integer intValue() {
		return integerValue.getValue();
	}

	public Double doubleValue() {
		return doubleValue.getValue();
	}

	public Property<BigDecimal> valueProperty() {
		return value;
	}

	public Property<Integer> integerProperty() {
		return integerValue;
	}

	public Property<Double> doubleProperty() {
		return doubleValue;
	}

	public void setPromptText(String promptText) {
		this.promptText = promptText;
		textField.setPromptText(promptText);
		updateView();
	}

	public String getPromptText() {
		return promptText;
	}

	public void setEditable(boolean editable) {
		textField.setEditable(editable);
	}

	public boolean isEditable() {
		return textField.isEditable();
	}

	public void setEnabled(boolean enabled) {
		textField.setDisable(!enabled);
	}

	public boolean isEnabled() {
		return !textField.isDisable();
	}

	public void setFractionDigits(int n) {
		format.setMaximumFractionDigits(n);
//		format.setMinimumFractionDigits(n);
		format.setParseIntegerOnly(n == 0);
		context = new MathContext(n);
		updateView();
	}

	public int getFractionDigits() {
		return format.getMaximumFractionDigits();
	}

	public void setUnits(String units) {
		unitLabel.setText(units);
	}

	public String getUnits() {
		return unitLabel.getText();
	}

	public boolean isNotNull() {
		return getValue() != null;
	}

	private void parseValue() {
		final String text = textField.getText();
		try {
			if (text == null || text.isEmpty()) {
				value.setValue(null);
			} else {
				final Number number = format.parse(text);
				value.setValue(new BigDecimal(number.toString(), context));
			}
			box.setStyle(null);
			updateView();
		} catch (NumberFormatException | ParseException e) {
			box.setStyle("-fx-border-color: tomato");
			System.err.println(e.getMessage());
		}
	}

	public void setOnAction(EventHandler<ActionEvent> handler) {
		textField.setOnAction(handler);
	}

	private void updateView() {
		if (value.getValue() == null) {
			textField.setText(null);
		} else {
			final String text = format.format(value.getValue());
			textField.setText(text);
		}
	}
}
