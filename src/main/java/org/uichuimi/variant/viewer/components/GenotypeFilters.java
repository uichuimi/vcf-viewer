package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.GenotypeType;
import htsjdk.variant.vcf.VCFHeader;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import org.controlsfx.control.CheckComboBox;
import org.uichuimi.variant.viewer.filter.SampleFilter;
import org.uichuimi.variant.viewer.filter.VariantContextFilter;
import org.uichuimi.variant.viewer.index.VcfIndex;
import org.uichuimi.variant.viewer.utils.NoArgFunction;

import java.util.List;

public class GenotypeFilters {

	@FXML
	private CheckComboBox<GenotypeType> genotypes;
	@FXML
	private ComboBox<Accessor> accessor;
	@FXML
	private CustomNumericField number;
	@FXML
	private CheckComboBox<String> samples;

	@FXML
	private ListView<SampleFilter> filters;

	private NoArgFunction onFilter = NoArgFunction.NO_OP;

	public void setMetadata(VCFHeader header) {
		samples.getItems().setAll(header.getGenotypeSamples());
		filters.getItems().clear();
	}

	public void setMetadata(VcfIndex index) {
	}

	public void setOnFilter(final NoArgFunction onFilter) {
		this.onFilter = onFilter;
	}

	@FXML
	private void initialize() {
		accessor.getItems().setAll(Accessor.values());
		accessor.valueProperty().addListener((obs, prev, accessor) -> number.setDisable(accessor != Accessor.ANY));
		genotypes.getItems().setAll(GenotypeType.values());

		samples.getCheckModel().clearChecks();
		genotypes.getCheckModel().clearChecks();
		accessor.setValue(Accessor.ALL);
		number.setValue(null);

		filters.setCellFactory(param -> new FilterCell<>());
		filters.setSelectionModel(new NoSelectionModel<>());
		filters.getItems().addListener((ListChangeListener<SampleFilter>) observable -> onFilter.call());

	}

	public VariantContextFilter getFilter() {
		final List<SampleFilter> filters = List.copyOf(this.filters.getItems());
		return variant -> filters.stream().allMatch(f -> f.filter(variant));
	}

	public void add() {
		filters.getItems().add(new SampleFilter(
			List.copyOf(samples.getCheckModel().getCheckedItems()),
			List.copyOf(genotypes.getCheckModel().getCheckedItems()),
			accessor.getValue(), number.intValue()));
		samples.getCheckModel().clearChecks();
		genotypes.getCheckModel().clearChecks();
		accessor.setValue(Accessor.ALL);
		number.setValue(null);
	}

}
