package org.uichuimi.variant.viewer.utils;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class ActiveListener<T> implements ChangeListener<T> {

	private boolean active;
	private final ChangeListener<T> listener;

	public ActiveListener(ChangeListener<T> listener) {
		this.listener = listener;
	}

	@Override
	public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
		if (active) listener.changed(observable, oldValue, newValue);
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive() {
		setActive(true);
	}

	public void setInactive() {
		setActive(false);
	}
}
