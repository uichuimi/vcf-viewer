package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFContigHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import htsjdk.variant.vcf.VCFSimpleHeaderLine;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.uichuimi.variant.VcfIndex;
import org.uichuimi.variant.viewer.components.filter.Field;
import org.uichuimi.variant.viewer.components.filter.FieldBuilder;
import org.uichuimi.variant.viewer.components.filter.Filter;
import org.uichuimi.variant.viewer.components.filter.Operator;
import org.uichuimi.variant.viewer.utils.Constants;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class PropertyFilters {

	public static final String FONT_AWESOME = "FontAwesome";
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
	private ListView<Filter> filters;

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

	/**
	 * Creates filters by using a custom index, which is optimized to improve performance. It also contains all the
	 * options for fields.
	 *
	 * @param index source index
	 */
	public void setMetadata(final VcfIndex index) {
		this.field.getItems().setAll(index.getFields());
	}

	public ObservableList<Filter> filter() {
		return filters.getItems();
	}

	@FXML
	private void add() {
		if (operator == null) return;
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
		final Filter filter = new Filter(field.getValue(), accessor.getValue(), operator.getValue(), value);
		System.out.println(filter);
		filters.getItems().add(filter);
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

		filters.setCellFactory(val -> new FilterCell());

		// Kick out entry controls
		valueHolder.getChildren().clear();
	}

	private void updateOptions() {
		final Field field = this.field.getValue();
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
					multipleEntry.getItems().setAll(field.getOptions());
					multipleEntry.getCheckModel().clearChecks();
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

	private class FilterCell extends ListCell<Filter> {
		private final Button delete = new Button(null, new Glyph(FONT_AWESOME, FontAwesome.Glyph.MINUS_CIRCLE));

		public FilterCell() {
			delete.setFocusTraversable(false);
			delete.getStyleClass().add("icon-button");
			delete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}

		@Override
		protected void updateItem(final Filter filter, final boolean empty) {
			super.updateItem(filter, empty);
			setText(null);
			if (empty || filter == null) {
				setGraphic(null);
			} else {
				final BorderPane content = new BorderPane();
				content.setLeft(new BorderPane(new SelectableLabel(filter.display())));
				content.setRight(delete);
				delete.setOnAction(event -> PropertyFilters.this.filters.getItems().remove(filter));
				setGraphic(content);
			}
		}
	}
}
