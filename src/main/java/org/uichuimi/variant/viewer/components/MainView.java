package org.uichuimi.variant.viewer.components;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.uichuimi.variant.viewer.utils.ViewTransitioner;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

public class MainView {

	private static BorderPane staticBorderPane;
	@FXML
	private ProgressBar progress;
	@FXML
	private Label message;
	@FXML
	private BorderPane center;

	private static final TaskManager manager = new TaskManager();

	public static void setView(View view) {
		setView(view, null);
	}

	public static void setView(View view, ViewTransitioner onLoad) {
		final Parent root = view.getRoot();
		final Object controller = view.getController();
		staticBorderPane.setCenter(root);
		if (onLoad != null) onLoad.transition(root, controller);
	}

	public static void error(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	public static void error(Exception e) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(e.getMessage());

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String exceptionText = sw.toString();

		Label label = new Label("Detalles del error:");

		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);

		alert.getDialogPane().setExpandableContent(expContent);
		alert.showAndWait();
	}

	public static boolean ask(String question) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setHeaderText(null);
		alert.setContentText(question);
		alert.setResizable(true);

		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && result.get() == ButtonType.OK;
	}

	public static void launch(Task<?> task) {
		manager.addToQueue(task);
	}
	@FXML
	private void initialize() {
		staticBorderPane = center;
		manager.taskProperty().addListener((obs, prev, task) -> {
			message.textProperty().unbind();
			progress.progressProperty().unbind();
			if (task == null) {
				message.setText(null);
				message.setVisible(false);
				progress.setProgress(0.0);
				progress.setVisible(false);
			} else {
				message.textProperty().bind(task.messageProperty());
				message.setVisible(true);
				progress.progressProperty().bind(task.progressProperty());
				progress.setVisible(true);
			}
		});
	}
}
