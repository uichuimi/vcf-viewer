package org.uichuimi.variant.viewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

public class Main extends Application {

	private static Window window;

	public static Window getWindow() {
		return window;
	}

	@Override
	public void start(final Stage stage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("components/main-view.fxml"));

		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("css/default.css").toExternalForm());

		stage.setTitle("JavaFX and Gradle");
		stage.setScene(scene);
		window = stage.getOwner();
		stage.show();

	}
}
