package org.uichuimi.variant.viewer.components;

import htsjdk.samtools.util.Interval;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFContigHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import htsjdk.variant.vcf.VCFSimpleHeaderLine;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckComboBox;
import org.uichuimi.variant.viewer.filter.*;
import org.uichuimi.variant.viewer.index.VcfIndex;
import org.uichuimi.variant.viewer.utils.Constants;

import java.util.*;
import java.util.stream.Collectors;

public class PropertyFilters {

	public static final String FONT_AWESOME = "FontAwesome";
	@FXML
	private CheckBox strict;
	@FXML
	private TextField textEntry;
	@FXML
	private TextField integerEntry;
	@FXML
	private TextField floatEntry;
	@FXML
	private CheckComboBox<String> multipleEntry;

	@FXML
	private VBox valueHolder;
	@FXML
	private ComboBox<Field> field;
	@FXML
	private ComboBox<Accessor> accessor;
	@FXML
	private ComboBox<Operator> operator;
	@FXML
	private ListView<AttributeFilter> filters;

	private VCFHeader header;

	/**
	 * Creates filters based on vcf header. Fields are created from {@link htsjdk.variant.vcf.VCFInfoHeaderLine}.
	 * Options cannot be read.
	 *
	 * @param header vcf header
	 */
	public void setMetadata(final VCFHeader header) {
		this.header = header;
		createBasicFilters();
	}

	private void createBasicFilters() {
		field.getItems().clear();
		field.getItems().addAll(chromField(), posField(), filterField(), qualField(), idField());
		for (final VCFInfoHeaderLine line : header.getInfoHeaderLines()) {
			field.getItems().add(toField(line));
		}
	}

	private Field chromField() {
		final List<String> contigs = header.getContigLines().stream().map(VCFContigHeaderLine::getID).collect(Collectors.toList());
		return new Field(Field.Type.TEXT, contigs, Constants.CHROM, false, Field.Category.STANDARD);
	}

	private Field posField() {
		return new Field(Field.Type.INTEGER, List.of(), Constants.POS, false, Field.Category.STANDARD);
	}

	private Field filterField() {
		final List<String> fltrs = header.getFilterLines().stream().map(VCFSimpleHeaderLine::getID).collect(Collectors.toList());
		return new Field(Field.Type.TEXT, fltrs, Constants.FILTER, true, Field.Category.STANDARD);
	}

	private Field qualField() {
		return new Field(Field.Type.FLOAT, List.of(), Constants.QUAL, false, Field.Category.STANDARD);
	}

	private Field idField() {
		return new Field(Field.Type.TEXT, List.of(), Constants.ID, false, Field.Category.STANDARD);
	}

	private Field toField(final VCFInfoHeaderLine line) {
		return FieldBuilder.create(line);
	}

	public boolean filter(final VariantContext context) {
		return filters.getItems().stream().allMatch(filter -> filter.filter(context));
	}

	public VariantContextFilter getFilter() {
		return new PropertiesFilter(new ArrayList<>(filters.getItems()));
	}

	/**
	 * Creates filters by using a custom index, which is optimized to improve performance. It also contains all the
	 * options for fields.
	 *
	 * @param index source index
	 */
	public void setMetadata(final VcfIndex index) {
		this.field.getItems().setAll(index.getFields());
	}

	public ObservableList<AttributeFilter> filterList() {
		return filters.getItems();
	}

	@FXML
	private void add() {
		if (operator == null) return;
		if (field.getValue() == null) return;
		if (field.getValue().isList() && accessor == null) return;
		final Object value;
		if (field.getValue().getType() == Field.Type.INTEGER) {
			try {
				value = Integer.parseInt(integerEntry.getText());
			} catch (NumberFormatException e) {
				return;
			}
		} else if (field.getValue().getType() == Field.Type.FLOAT) {
			try {
				value = Double.parseDouble(floatEntry.getText());
			} catch (NumberFormatException e) {
				return;
			}
		} else if (field.getValue().getType() == Field.Type.TEXT) {
			if (field.getValue().getOptions().isEmpty()) {
				if (StringUtils.isBlank(textEntry.getText())) return;
				value = textEntry.getText();
			} else {
				if (multipleEntry.getCheckModel().getCheckedItems().isEmpty()) return;
				value = new TreeSet<>(multipleEntry.getCheckModel().getCheckedItems());
			}
		} else if (field.getValue().getType() == Field.Type.FLAG) {
			value = false;
		} else return;

		final Collection<Interval> intervals = createInterval(field.getValue(), value);
		final AttributeFilter filter = new AttributeFilter(field.getValue(), accessor.getValue(), operator.getValue(), value, strict.isSelected(), intervals);
		System.out.println(filter);
		filters.getItems().add(filter);
	}

	private Collection<Interval> createInterval(Field field, Object value) {
		final Collection<Interval> intervals = new LinkedHashSet<>();
		if (field.getCategory() == Field.Category.STANDARD && field.getName().equals(Constants.CHROM)) {
			final Collection<String> chroms = (Collection<String>) value;
			for (String chrom : chroms) {
				for (VCFContigHeaderLine line : header.getContigLines()) {
					if (line.getID().equals(chrom)) {
						intervals.add(createInterval(line));
					}
				}
			}
		}
		return List.copyOf(intervals);
	}

	private Interval createInterval(VCFContigHeaderLine line) {
		final String lengthString = line.getGenericFields().get("length");
		final int length = lengthString == null ? 0 : Integer.parseInt(lengthString);
		return new Interval(line.getID(), 1, length);
	}

	@FXML
	private void initialize() {
		field.setCellFactory(view -> new FieldListCell());
		field.setButtonCell(new FieldListCell());

		operator.setCellFactory(view -> new OperatorListCell());
		operator.setButtonCell(new OperatorListCell());

		accessor.getItems().setAll(Accessor.values());
		accessor.setValue(Accessor.ANY);

		field.valueProperty().addListener((obs, prev, value) -> updateOptions());

		integerEntry.textProperty().addListener((observableValue, s, val) -> {
			try {
				Integer.parseInt(val);
				integerEntry.getStyleClass().remove("error");
			} catch (NumberFormatException ex) {
				integerEntry.getStyleClass().add("error");
			}
		});
		floatEntry.textProperty().addListener((observableValue, s, val) -> {
			try {
				Double.parseDouble(val);
				floatEntry.getStyleClass().remove("error");
			} catch (NumberFormatException ex) {
				floatEntry.getStyleClass().add("error");
			}
		});

		filters.setCellFactory(val -> new FilterCell<>());
		filters.setSelectionModel(new NoSelectionModel<>());

		// Kick out entry controls
		// NOTE: the reason for these controls to be initialised inside the valueHolder
		// is to keep the traverse order
		valueHolder.getChildren().clear();

		integerEntry.setOnAction(event -> add());
		floatEntry.setOnAction(event -> add());
		textEntry.setOnAction(event -> add());
	}

	private void updateOptions() {
		final Field field = this.field.getValue();
		if (field == null) {
			accessor.setDisable(true);
			operator.setDisable(true);
			valueHolder.getChildren().clear();
		}
		// Enable/disable accessor
		accessor.setDisable(!field.isList());

		// Set operators and select first
		operator.getItems().setAll(field.getOperators());
		operator.setValue(field.getOperators().iterator().next());
		operator.setDisable(false);

		// Set value box
		switch (field.getType()) {
			case FLOAT -> {
				valueHolder.getChildren().setAll(floatEntry);
				floatEntry.setText("");

			}
			case INTEGER -> {
				valueHolder.getChildren().setAll(integerEntry);
				integerEntry.setText("");
			}
			case TEXT -> {
				if (field.getOptions().isEmpty()) {
					valueHolder.getChildren().setAll(textEntry);
					textEntry.setText(null);
				} else {
					valueHolder.getChildren().setAll(multipleEntry);
					multipleEntry.getCheckModel().clearChecks();
					multipleEntry.getItems().setAll(field.getOptions());
					multipleEntry.setShowCheckedCount(true);
					operator.getItems().setAll(Operator.TEXT_EQUAL);
					operator.setValue(Operator.TEXT_EQUAL);
					operator.setDisable(true);
				}
			}
			case FLAG -> valueHolder.getChildren().clear();
		}
	}

	private static class FieldListCell extends ListCell<Field> {

		@Override
		protected void updateItem(final Field item, final boolean empty) {
			super.updateItem(item, empty);
			setText(item == null || empty ? null : "[%s] %s".formatted(item.getCategory(), item.getName()));
		}
	}

	private static class OperatorListCell extends ListCell<Operator> {
		@Override
		protected void updateItem(final Operator item, final boolean empty) {
			super.updateItem(item, empty);
			setText(item == null || empty ? null : item.getDisplay());
		}
	}

}
