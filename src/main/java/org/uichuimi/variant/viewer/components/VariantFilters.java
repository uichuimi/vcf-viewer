package org.uichuimi.variant.viewer.components;

import htsjdk.variant.vcf.VCFHeader;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.uichuimi.variant.viewer.filter.AttributeFilter;
import org.uichuimi.variant.viewer.filter.BaseFilter;
import org.uichuimi.variant.viewer.index.VcfIndex;
import org.uichuimi.variant.viewer.utils.NoArgFunction;

import java.util.ArrayList;
import java.util.List;

public class VariantFilters {

	@FXML
	@SuppressWarnings({"unused"})
	private BorderPane propertyFilters;
	@FXML
	@SuppressWarnings({"unused"})
	private PropertyFilters propertyFiltersController;
	@FXML
	@SuppressWarnings({"unused"})
	private GenotypeFilters genotypeFiltersController;
	@FXML
	@SuppressWarnings({"unused"})
	private BorderPane genotypeFilters;
	@FXML
	private VBox propertyFiltersPane;
	@FXML
	private VBox genotypeFiltersPane;
	@FXML
	private FlowPane showing;
	@FXML
	private BorderPane root;
	@FXML
	private SplitPane content;
	private NoArgFunction onReload;


	private List<SelectablePane> panes;

	@FXML
	private void initialize() {
		propertyFiltersController.filterList().addListener((ListChangeListener<AttributeFilter>) change -> onReload.call());
		genotypeFiltersController.setOnFilter(() -> onReload.call());

		panes = List.of(
			new SelectablePane("PROPERTIES", propertyFiltersPane),
			new SelectablePane("GENOTYPES", genotypeFiltersPane));
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

	public void setMetadata(VcfIndex index) {
		propertyFiltersController.setMetadata(index);
		genotypeFiltersController.setMetadata(index);
	}

	public void setMetadata(VCFHeader header) {
		propertyFiltersController.setMetadata(header);
		genotypeFiltersController.setMetadata(header);
	}

	public List<BaseFilter> getFilters() {
		final ArrayList<BaseFilter> filters = new ArrayList<>();
		filters.addAll(genotypeFiltersController.getFilters());
		filters.addAll(propertyFiltersController.filterList());
		return filters;
	}

	public void setOnReload(NoArgFunction onReload) {
		this.onReload = onReload;
	}
}
