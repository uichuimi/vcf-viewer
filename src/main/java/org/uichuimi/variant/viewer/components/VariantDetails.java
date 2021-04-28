package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class VariantDetails {

	@FXML
	@SuppressWarnings("unused")
	private BorderPane properties;
	@FXML
	private PropertiesTable propertiesController;

	@FXML
	@SuppressWarnings("unused")
	private BorderPane genotypes;
	@FXML
	private GenotypesTable genotypesController;

	@FXML
	@SuppressWarnings("unused")
	private Parent frequencies;
	@FXML
	private FrequenciesTable frequenciesController;

	@FXML
	private BorderPane root;
	@FXML
	private FlowPane showing;
	@FXML
	private VBox propertiesPane;
	@FXML
	private VBox frequenciesPane;
	@FXML
	private VBox genotypesPane;
	@FXML
	private SplitPane content;

	private List<SelectablePane> panes;

	@FXML
	private void initialize() {
		panes = List.of(
			new SelectablePane("PROPERTIES", propertiesPane),
			new SelectablePane("FREQUENCIES", frequenciesPane),
			new SelectablePane("GENOTYPES", genotypesPane));
		for (SelectablePane selectablePane : panes) {
			final ToggleButton button = new ToggleButton(selectablePane.getName());
			button.setStyle("-fx-font-size: 0.8em");
			button.setRotate(90);
			button.selectedProperty().bindBidirectional(selectablePane.showingProperty());
			showing.getChildren().add(new Group(button));
			selectablePane.showingProperty().addListener((observable, oldValue, newValue) -> update());
		}
	}

	private void update() {
		content.getItems().clear();
		for (SelectablePane pane : panes) {
			if (pane.isShowing()) {
				content.getItems().add(pane.getParent());
			}
		}
		if (content.getItems().isEmpty()) {
			root.setCenter(null);
			root.setMaxWidth(0);
		} else {
			root.setCenter(content);
			root.setMaxWidth(BorderPane.USE_PREF_SIZE);
		}
		root.requestLayout();
	}

	public void set(VariantContext variant) {
		propertiesController.select(variant);
		genotypesController.select(variant);
		frequenciesController.select(variant);
	}

	public void setHeader(VCFHeader header) {
		frequenciesController.setHeader(header);
		final List<String> fields = frequenciesController.frequencyFields();
		propertiesController.setIgnored(fields);
	}
}
