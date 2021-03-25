package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Map;

public class PropertiesTable {

	@FXML
	private TableView<Map.Entry<String, Object>> infoTable;
	@FXML
	private TableColumn<Map.Entry<String, Object>, String> info;
	@FXML
	private TableColumn<Map.Entry<String, Object>, String> value;

	public void select(final VariantContext variant) {
		infoTable.getItems().clear();
		if (variant == null) return;
		for (final Map.Entry<String, Object> entry : variant.getCommonInfo().getAttributes().entrySet()) {
			infoTable.getItems().add(entry);
		}
	}

	@FXML
	private void initialize() {
		infoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		info.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getKey()));
		value.setCellValueFactory(features -> new SimpleObjectProperty<>(String.valueOf(features.getValue().getValue())));
		value.setCellFactory(column -> new NaturalCell<>());
		infoTable.setEditable(true);
	}

}
