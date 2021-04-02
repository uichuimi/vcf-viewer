package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeType;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import org.uichuimi.variant.VcfIndex;
import org.uichuimi.variant.viewer.utils.ActiveListener;
import org.uichuimi.variant.viewer.utils.BitUtils;
import org.uichuimi.variant.viewer.utils.Constants;
import org.uichuimi.variant.viewer.utils.NoArgFunction;

import java.util.EnumMap;
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

	private final ActiveListener<Boolean> CHANGE_LISTENER = new ActiveListener<>((obs, prev, selected) -> changed());
	private VcfIndex index;
	private long[] mask;

	public void setMetadata(VCFHeader header) {
		filters = header.getGenotypeSamples().stream().map(GtFilter::new).collect(Collectors.toList());
		CHANGE_LISTENER.setInactive();
		filters.stream().flatMap(gtFilter -> Stream.of(
			gtFilter.getGt(GenotypeType.NO_CALL),
			gtFilter.getGt(GenotypeType.HOM_REF),
			gtFilter.getGt(GenotypeType.HET),
			gtFilter.getGt(GenotypeType.HOM_VAR)))
			.forEach(booleanProperty -> booleanProperty.addListener(CHANGE_LISTENER));
		CHANGE_LISTENER.setActive();
		filterSampleTable();
		mask = createMask();
	}

	@FXML
	private void filterSampleTable() {
		sampleTable.getItems().setAll(GtFilter.ALL);
		final String term = this.search.getText().toLowerCase();
		filters.stream()
			.filter(gtFilter -> gtFilter.sample.toLowerCase().contains(term))
			.forEach(sampleTable.getItems()::add);
	}

	public void setMetadata(VcfIndex index) {
		this.index = index;
	}

	public boolean filter(final VariantContext variant, int position) {
		if (index == null)
			return filterByHeader(variant);
		else
			return filterByIndex(position);
	}

	private boolean filterByHeader(final VariantContext variant) {
		for (final GtFilter filter : filters) {
			final Genotype genotype = variant.getGenotype(filter.sample);
			final GenotypeType type = genotype.getType();
			if (!filter.getGt(type).getValue()) return false;
		}
		return true;
	}

	private boolean filterByIndex(final int position) {
		// Mask is inverted, that means if we look for a NO_CALL in a given sample, mask is 0111 for that person.
		// We then intersect the bitset of that person with the mask. If, and only if, the intersection is false (0),
		// we can assume that the filter passes.
		// This method  (inverse mask and !intersection) allows to query 16 samples with a single bitwise operation,
		// since one single person that do not match the mask will result in a > 0 intersection.
		return !BitUtils.intersects(mask, index.getGts().get(position));
	}

	public void setOnFilter(final NoArgFunction onFilter) {
		this.onFilter = onFilter;
	}

	private long[] createMask() {
		final int numberOfWords = (int) Math.ceil(filters.size() * 4. / 64);
		final long[] mask = new long[numberOfWords];
		for (int i = 0; i < filters.size(); i++) {
			final GtFilter filter = filters.get(i);
			for (int pos = 0; pos < Constants.validGenotypeTypes().size(); pos++) {
				final int offset = i * Constants.validGenotypeTypes().size() + pos;
				if (filter.getGt(Constants.validGenotypeTypes().get(pos)).getValue()) {
					BitUtils.set(mask, offset);
				} else {
					BitUtils.clear(mask, offset);
				}
			}
		}
		BitUtils.flip(mask);
		return mask;
	}

	@FXML
	private void initialize() {
		noCall.setCellValueFactory(feats -> feats.getValue().getGt(GenotypeType.NO_CALL));
		homRef.setCellValueFactory(feats -> feats.getValue().getGt(GenotypeType.HOM_REF));
		het.setCellValueFactory(feats -> feats.getValue().getGt(GenotypeType.HET));
		homVar.setCellValueFactory(feats -> feats.getValue().getGt(GenotypeType.HOM_VAR));

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
			gtFilter3 -> gtFilter3.getGt(GenotypeType.NO_CALL),
			gtFilter -> gtFilter.getGt(GenotypeType.HOM_REF),
			gtFilter1 -> gtFilter1.getGt(GenotypeType.HET),
			gtFilter2 -> gtFilter2.getGt(GenotypeType.HOM_VAR));
		for (final Function<GtFilter, Property<Boolean>> getter : getters) {
			// Listen to changes in GtFilter.ALL and apply new value to all filters.
			// After this, re-filter
			getter.apply(GtFilter.ALL).addListener((obs, prev, selected) -> {
				CHANGE_LISTENER.setInactive();
				for (final GtFilter filter : filters) {
					getter.apply(filter).setValue(selected);
				}
				CHANGE_LISTENER.setActive();
				changed();
			});
		}
	}

	private void changed() {
		onFilter.call();
		mask = createMask();
	}

	private static class GtFilter {
		static final GtFilter ALL = new GtFilter("_ALL_");
		private final String sample;
		private final EnumMap<GenotypeType, Property<Boolean>> gtMap = new EnumMap<>(GenotypeType.class);

		public GtFilter(final String sample) {
			this.sample = sample;
		}

		public String getSample() {
			return sample;
		}

		public Property<Boolean> getGt(GenotypeType type) {
			return gtMap.computeIfAbsent(type, t -> new SimpleBooleanProperty(true));
		}

	}


}
