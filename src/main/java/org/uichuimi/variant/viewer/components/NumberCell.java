package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

import java.text.NumberFormat;
import java.util.Locale;

public class NumberCell extends TableCell<VariantContext, Double> {

	private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
	private final SelectableLabel label = new SelectableLabel();

	public NumberCell(final int decimals) {
		numberFormat.setMaximumFractionDigits(decimals);
		label.setAlignment(Pos.CENTER_RIGHT);
		label.focusedProperty().addListener((obs, prev, focused) -> {
			if (focused) getTableView().getSelectionModel().select(getIndex());
		});
	}

	@Override
	protected void updateItem(final Double item, final boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			setGraphic(null);
		} else {
			label.setText(numberFormat.format(item));
			setGraphic(label);
		}
	}
}
