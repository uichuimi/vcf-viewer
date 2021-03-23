package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import javafx.scene.control.TableCell;

public class CoordinateCell extends TableCell<VariantContext, VariantContext> {

	@Override
	protected void updateItem(final VariantContext variant, final boolean empty) {
		super.updateItem(variant, empty);
		if (empty || variant == null) {
			setText(null);
			setGraphic(null);
		} else {
			setText(String.format("%s:%,d", variant.getContig(), variant.getStart()));
		}
	}
}
