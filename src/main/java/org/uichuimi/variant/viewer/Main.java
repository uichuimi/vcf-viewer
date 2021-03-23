package org.uichuimi.variant.viewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(final Stage stage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("components/welcome.fxml"));

		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("css/default.css").toExternalForm());

		stage.setTitle("JavaFX and Gradle");
		stage.setScene(scene);
		stage.show();

	}
}
