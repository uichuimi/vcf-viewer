package org.uichuimi.variant.viewer.components;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import java.util.ArrayDeque;
import java.util.Queue;

public class TaskManager {


	private final Queue<Task<?>> tasks = new ArrayDeque<>();
	private final Property<Task<?>> task = new SimpleObjectProperty<>();

	public TaskManager() {
		Runtime.getRuntime().addShutdownHook(new Thread(this::destroyTasks));
	}

	public void destroyTasks() {
		tasks.clear();
		Platform.runLater(() -> {
			if (task.getValue() != null && task.getValue().isRunning())
				task.getValue().cancel();
		});
	}

	public void addToQueue(Task<?> task) {
		tasks.add(task);
		next();
	}

	public Property<Task<?>> taskProperty() {
		return task;
	}

	private void next() {
		if (task.getValue() == null && !tasks.isEmpty())
			launch(tasks.poll());
	}

	private void launch(Task<?> task) {
		task.stateProperty().addListener((observable, oldValue, state) -> {
			if (state == Worker.State.SUCCEEDED || state == Worker.State.CANCELLED || state == Worker.State.FAILED) {
				this.task.setValue(null);
				next();
			}
			if (task.getException() != null) {
				task.getException().printStackTrace();
			}
		});
		this.task.setValue(task);
		final Thread thread = new Thread(task);
		thread.start();
	}
}
