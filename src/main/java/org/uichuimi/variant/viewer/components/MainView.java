package org.uichuimi.variant.viewer.components;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.util.function.BiConsumer;

public class MainView {

	private static BorderPane staticBorderPane;

	@FXML
	private BorderPane center;

	public static void setView(View view) {
		setView(view, null);
	}

	public static void setView(View view, BiConsumer<Parent, Object> onLoad) {
		final Parent root = view.getRoot();
		final Object controller = view.getController();
		staticBorderPane.setCenter(root);
		if (onLoad != null) onLoad.accept(root, controller);
	}

	@FXML
	private void initialize() {
		staticBorderPane = center;
	}
}
