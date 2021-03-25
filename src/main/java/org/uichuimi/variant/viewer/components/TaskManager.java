package org.uichuimi.variant.viewer.components;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import java.util.PriorityQueue;
import java.util.Queue;

public class TaskManager {


	private final Queue<Task<?>> tasks = new PriorityQueue<>();
	private final Property<Task<?>> task = new SimpleObjectProperty<>();


	public void addToQueue(Task<?> task) {
		tasks.add(task);
		next();
	}

	public void destroyTasks() {
		tasks.clear();
		if (task.getValue() != null && task.getValue().isRunning())
			task.getValue().cancel();
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
		});
		this.task.setValue(task);
		new Thread(task).start();
	}


	public Property<Task<?>> taskProperty() {
		return task;
	}
}
