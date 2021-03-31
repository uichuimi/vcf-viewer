package org.uichuimi.variant.viewer.components;

import htsjdk.variant.vcf.VCFHeader;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import org.uichuimi.variant.viewer.utils.ActiveListener;
import org.uichuimi.variant.viewer.utils.NoArgFunction;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenotypeFilters {
	@FXML
	private TextField search;
	@FXML
	private TableView<GtFilter> sampleTable;
	@FXML
	private TableColumn<GtFilter, String> sample;
	@FXML
	private TableColumn<GtFilter, Boolean> noCall;
	@FXML
	private TableColumn<GtFilter, Boolean> het;
	@FXML
	private TableColumn<GtFilter, Boolean> homVar;
	@FXML
	private TableColumn<GtFilter, Boolean> homRef;

	private List<GtFilter> filters;

	private NoArgFunction onFilter = NoArgFunction.NO_OP;

	private final ActiveListener<Boolean> CHANGE_LISTENER = new ActiveListener<>((obs, prev, selected) -> onFilter.call());


	public void setMetadata(VCFHeader header) {
		filters = header.getGenotypeSamples().stream().map(GtFilter::new).collect(Collectors.toList());
		filters.stream().flatMap(gtFilter -> Stream.of(
			gtFilter.noCallProperty(),
			gtFilter.homRefProperty(),
			gtFilter.hetProperty(),
			gtFilter.homVarProperty()))
			.forEach(booleanProperty -> booleanProperty.addListener(CHANGE_LISTENER));
		filterSampleTable();
	}

	@FXML
	private void filterSampleTable() {
		sampleTable.getItems().setAll(GtFilter.ALL);
		final String term = this.search.getText().toLowerCase();
		filters.stream()
			.filter(gtFilter -> gtFilter.sample.toLowerCase().contains(term))
			.forEach(sampleTable.getItems()::add);
	}

	public void setOnFilter(final NoArgFunction onFilter) {
		this.onFilter = onFilter;
	}

	@FXML
	private void initialize() {
		noCall.setCellValueFactory(feats -> feats.getValue().noCallProperty());
		homRef.setCellValueFactory(feats -> feats.getValue().homRefProperty());
		het.setCellValueFactory(feats -> feats.getValue().hetProperty());
		homVar.setCellValueFactory(feats -> feats.getValue().homVarProperty());

		noCall.setCellFactory(CheckBoxTableCell.forTableColumn(noCall));
		homRef.setCellFactory(CheckBoxTableCell.forTableColumn(homRef));
		het.setCellFactory(CheckBoxTableCell.forTableColumn(het));
		homVar.setCellFactory(CheckBoxTableCell.forTableColumn(homVar));

		sample.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getSample()));

		sampleTable.setEditable(true);

		bindSelectAll();
	}

	private void bindSelectAll() {
		final List<Function<GtFilter, Property<Boolean>>> getters = List.of(
			GtFilter::noCallProperty,
			GtFilter::homRefProperty,
			GtFilter::hetProperty,
			GtFilter::homVarProperty);
		for (final Function<GtFilter, Property<Boolean>> getter : getters) {
			// Listen to changes in GtFilter.ALL and apply new value to all filters.
			// After this, re-filter
			getter.apply(GtFilter.ALL).addListener((obs, prev, selected) -> {
				CHANGE_LISTENER.setInactive();
				for (final GtFilter filter : filters) {
					getter.apply(filter).setValue(selected);
				}
				CHANGE_LISTENER.setActive();
				onFilter.call();
			});
		}
	}

	private static class GtFilter {
		static final GtFilter ALL = new GtFilter("_ALL_");
		private final String sample;
		private final Property<Boolean> homRef = new SimpleBooleanProperty(true);
		private final Property<Boolean> het = new SimpleBooleanProperty(true);
		private final Property<Boolean> homVar = new SimpleBooleanProperty(true);
		private final Property<Boolean> noCall = new SimpleBooleanProperty(true);

		public GtFilter(final String sample) {
			this.sample = sample;
		}

		public String getSample() {
			return sample;
		}

		public Property<Boolean> homRefProperty() {
			return homRef;
		}

		public Property<Boolean> hetProperty() {
			return het;
		}

		public Property<Boolean> homVarProperty() {
			return homVar;
		}

		public Property<Boolean> noCallProperty() {
			return noCall;
		}
	}


}
