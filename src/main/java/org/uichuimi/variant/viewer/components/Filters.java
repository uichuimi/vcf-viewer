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
import org.controlsfx.control.CheckComboBox;
import org.uichuimi.variant.VcfIndex;
import org.uichuimi.variant.viewer.components.filter.Field;
import org.uichuimi.variant.viewer.components.filter.FieldBuilder;
import org.uichuimi.variant.viewer.components.filter.Filter;
import org.uichuimi.variant.viewer.components.filter.Operator;

import java.util.List;
import java.util.stream.Collectors;

public class Filters {

	private final TextField textEntry = new TextField();
	private final TextField integerEntry = new TextField();
	private final TextField floatEntry = new TextField();
	private final CheckComboBox<String> multipleEntry = new CheckComboBox<>();

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
		initFilters();
	}

	private void initFilters() {
		field.getItems().clear();
		field.getItems().addAll(chromField(), posField(), filterField(), qualField(), idField());
		for (final VCFInfoHeaderLine line : header.getInfoHeaderLines()) {
			field.getItems().addAll(toField(line));
		}
	}

	private Field chromField() {
		final List<String> contigs = header.getContigLines().stream().map(VCFContigHeaderLine::getID).collect(Collectors.toList());
		return new Field(Field.Type.MULTIPLE, contigs, VariantContext::getContig, "Chromosome", false);
	}

	private Field posField() {
		return new Field(Field.Type.INTEGER, null, VariantContext::getStart, "Position", false);
	}

	private Field filterField() {
		final List<String> fltrs = header.getFilterLines().stream().map(VCFSimpleHeaderLine::getID).collect(Collectors.toList());
		return new Field(Field.Type.MULTIPLE, fltrs, VariantContext::getFilters, "Filter", true);
	}

	private Field qualField() {
		return new Field(Field.Type.FLOAT, null, VariantContext::getPhredScaledQual, "Quality", false);
	}

	private Field idField() {
		return new Field(Field.Type.TEXT, null, VariantContext::getID, "Identifier", false);
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
		for (final Filter filter : filters.getItems()) {

		}
	}

	public ObservableList<Filter> filter() {
		return filters.getItems();
	}

	@FXML
	private void add() {
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
			value = textEntry.getText();
		} else if (field.getValue().getType() == Field.Type.MULTIPLE) {
			value = multipleEntry.getCheckModel().getCheckedItems();
		} else if (field.getValue().getType() == Field.Type.FLAG) {
			value = false;
		} else value = null;
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
	}

	private void updateOptions() {
		final Field field = this.field.getValue();
		// Set accessor
		accessor.setDisable(!field.isList());
		// Set operators
		operator.getItems().setAll(field.getOperators());
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
			case MULTIPLE -> {
				valueHolder.getChildren().setAll(multipleEntry);
				multipleEntry.getItems().setAll(field.getOptions());
				multipleEntry.getCheckModel().clearChecks();
			}
			case TEXT -> {
				valueHolder.getChildren().setAll(textEntry);
				textEntry.setText(null);
			}
			case FLAG -> valueHolder.getChildren().clear();
		}
	}

	private static class FieldListCell extends ListCell<Field> {

		@Override
		protected void updateItem(final Field item, final boolean empty) {
			super.updateItem(item, empty);
			setText(item == null || empty ? null : item.getDisplayName());
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
		private final Label delete = new Label("X");

		@Override
		protected void updateItem(final Filter filter, final boolean empty) {
			super.updateItem(filter, empty);
			setText(null);
			if (empty || filter == null) {
				setGraphic(null);
			} else {
				final BorderPane content = new BorderPane();
				content.setLeft(new SelectableLabel(filter.display()));
				content.setRight(delete);
				delete.setOnMouseClicked(event -> Filters.this.filters.getItems().remove(filter));
				setGraphic(content);
			}
		}
	}
}
