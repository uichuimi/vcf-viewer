package org.uichuimi.variant.viewer.components;

import javafx.fxml.FXML;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.uichuimi.variant.viewer.action.FileActions;

import java.io.File;
import java.util.List;

public class Welcome {

	public static final FileChooser.ExtensionFilter VCF = new FileChooser.ExtensionFilter("Variant Call Format", "*.vcf", "*.vcf.gz", "*.bcf");
	public static final String DRAG_OVER = "drag-over";

	@FXML
	private VBox drop;


	@FXML
	private void initialize() {
		// NOTE: drag sequence: entered -> over* [-> dropped] -> exited
		drop.setOnDragEntered(this::onDragEntered);
		drop.setOnDragOver(this::onDragOver);
		drop.setOnDragDropped(this::onDragDropped);
		drop.setOnDragExited(this::onDragExited);
		drop.setOnMouseClicked(this::open);
	}

	private void onDragEntered(final DragEvent event) {
		final File file = dragFile(event);
		if (isVcf(file)) {
			drop.getStyleClass().add(DRAG_OVER);
		}
	}

	private void onDragOver(final DragEvent event) {
		if (event.getGestureSource() != this && event.getDragboard().hasString())
			event.acceptTransferModes(TransferMode.LINK);
		event.consume();
	}

	private void onDragDropped(final DragEvent dragEvent) {
		final File file = dragFile(dragEvent);
		if (isVcf(file)) open(file);
	}

	private void onDragExited(final DragEvent dragEvent) {
		drop.getStyleClass().remove(DRAG_OVER);
	}

	private void open(MouseEvent event) {
		final File file = FileActions.openVcf();
		if (file != null) open(file);
	}

	private File dragFile(final DragEvent event) {
		final List<File> files = event.getDragboard().getFiles();
		return files == null || files.isEmpty() ? null : files.get(0);
	}

	private boolean isVcf(final File file) {
		return file != null && VCF.getExtensions().stream()
			.map(s -> s.replaceFirst("\\*", ""))
			.anyMatch(s -> file.getName().endsWith(s));
	}

	private void open(final File file) {
		MainView.setTitle(file.getName());
		MainView.setView(View.TABLE, (view, controller) -> ((VariantsTable) controller).setFile(file));
	}
}
