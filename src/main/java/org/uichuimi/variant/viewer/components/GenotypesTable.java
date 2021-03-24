package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeType;
import htsjdk.variant.variantcontext.VariantContext;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class GenotypesTable {

	@FXML
	private FlowPane selectors;
	@FXML
	private TableView<Genotype> table;
	@FXML
	private TableColumn<Genotype, String> alleles;
	@FXML
	private TableColumn<Genotype, String> sample;
	@FXML
	private TableColumn<Genotype, String> genotype;
	@FXML
	private TableColumn<Genotype, String> count;
	private VariantContext variant;

	public void select(final VariantContext variant) {
		this.variant = variant;
		filter();
	}

	private void filter() {
		if (variant == null) return;
		final Map<GenotypeType, Boolean> selected = new HashMap<>();
		for (final Node child : selectors.getChildren()) {
			selected.put((GenotypeType) child.getUserData(), ((ToggleButton) child).isSelected());
		}
		table.getItems().clear();
		for (final Genotype genotype : variant.getGenotypes()) {
			if (selected.get(genotype.getType())) {
				table.getItems().add(genotype);
			}
		}
	}

	@FXML
	private void initialize() {
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		sample.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getSampleName()));
		genotype.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getType().toString()));
		alleles.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getGenotypeString()));
		count.setCellValueFactory(features -> new SimpleObjectProperty<>(alleleCount(features)));
		for (final GenotypeType type : GenotypeType.values()) {
			final ToggleButton button = new ToggleButton(type.toString());
			button.setMnemonicParsing(false);
			button.getStyleClass().add("chip");
			button.setSelected(true);
			button.setUserData(type);
			button.selectedProperty().addListener((obs, prev, selected) -> filter());
			selectors.getChildren().add(button);
		}
	}

	private String alleleCount(final TableColumn.CellDataFeatures<Genotype, String> features) {
		final StringJoiner joiner = new StringJoiner(", ");
		final Genotype genotype = features.getValue();
		for (int i = 0; i < genotype.getAD().length; i++) {
			joiner.add(variant.getAlleles().get(i).getBaseString() + "=" + genotype.getAD()[i]);
		}
		return joiner.toString();
	}
}
