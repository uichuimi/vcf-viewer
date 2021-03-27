package org.uichuimi.variant.viewer.components;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class SelectableLabel extends Label {

	public SelectableLabel() {
		this(null);
	}

	public SelectableLabel(final String value) {
		StackPane textStack = new StackPane();
		TextField textField = new TextField(value);
		textField.setEditable(false);
		textField.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-background-radius: 0; -fx-padding: 0; -fx-border-width: 0");
		// the invisible label is a hack to get the textField to size like a label.
		Label label = new Label();
		label.textProperty().bind(textProperty());
		label.setVisible(false);
		textStack.getChildren().addAll(label, textField);
		textProperty().bindBidirectional(textField.textProperty());
		setGraphic(textStack);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		alignmentProperty().bindBidirectional(label.alignmentProperty());
		alignmentProperty().bindBidirectional(textField.alignmentProperty());
		textField.focusedProperty().addListener((observableValue, aBoolean, t1) -> setFocused(t1));
	}
}
