package org.uichuimi.variant.viewer.components;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public enum View {
	TABLE("variants-table.fxml");

	private final String path;
	private Object controller;
	private Parent root;

	View(final String path) {
		this.path = path;
	}

	public Parent getRoot() {
		if (root == null) load();
		return root;
	}

	public Object getController() {
		if (controller == null) load();
		return controller;
	}
	private void load() {
		final FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource(path));
		try {
			loader.load();
			controller = loader.getController();
			root = loader.getRoot();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
