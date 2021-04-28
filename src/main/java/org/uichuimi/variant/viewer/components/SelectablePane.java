package org.uichuimi.variant.viewer.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;

public class SelectablePane {

	private final String name;
	private final Parent parent;
	private final BooleanProperty showing = new SimpleBooleanProperty(true);

	public SelectablePane(String name, Parent parent) {
		this.name = name;
		this.parent = parent;
	}

	public Parent getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public BooleanProperty showingProperty() {
		return showing;
	}

	public boolean isShowing() {
		return showing.get();
	}
}
