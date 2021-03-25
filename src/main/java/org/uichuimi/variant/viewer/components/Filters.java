package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFContigHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import htsjdk.variant.vcf.VCFSimpleHeaderLine;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckComboBox;
import org.uichuimi.variant.viewer.components.filter.Field;
import org.uichuimi.variant.viewer.components.filter.FieldBuilder;
import org.uichuimi.variant.viewer.components.filter.Filter;
import org.uichuimi.variant.viewer.components.filter.Operator;

import java.util.List;
import java.util.stream.Collectors;

public class Filters {

	private final TextField textEntry = new TextField();
	private final TextField numberEntry = new TextField();
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
	private TableView<Filter> filters;
	private VCFHeader header;
	private boolean queryable;

	public void setMetadata(final VCFHeader header, final boolean queryable) {
		this.header = header;
		this.queryable = queryable;
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

	@FXML
	private void initialize() {
		field.setCellFactory(view -> new FieldListCell());
		field.setButtonCell(new FieldListCell());

		operator.setCellFactory(view -> new OperatorListCell());
		operator.setButtonCell(new OperatorListCell());

		accessor.getItems().setAll(Accessor.values());

		field.valueProperty().addListener((obs, prev, value) -> updateOptions());
	}

	private void updateOptions() {
		final Field field = this.field.getValue();
		// Set accessor
		accessor.setDisable(!field.isList());
		// Set operators
		operator.getItems().setAll(field.getOperators());
		// Set value box
		switch (field.getType()) {
			case FLOAT, INTEGER -> {
				valueHolder.getChildren().setAll(numberEntry);
				numberEntry.setText(null);
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
}
