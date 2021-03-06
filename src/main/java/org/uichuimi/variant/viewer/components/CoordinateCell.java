package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

public class CoordinateCell extends TableCell<VariantContext, VariantContext> {

	private final SelectableLabel label = new SelectableLabel();

	public CoordinateCell() {
		label.setAlignment(Pos.CENTER_RIGHT);
		label.focusedProperty().addListener((obs, prev, focused) -> {
			if (focused) getTableView().getSelectionModel().select(getIndex());
		});
	}

	@Override
	protected void updateItem(final VariantContext variant, final boolean empty) {
		super.updateItem(variant, empty);
		if (empty || variant == null) {
			setText(null);
			setGraphic(null);
		} else {
			label.setText(String.format("%s:%,d", variant.getContig(), variant.getStart()));
			setGraphic(label);
		}
	}
}
