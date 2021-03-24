package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;

public class AlleleCell extends javafx.scene.control.TableCell<VariantContext, String> {

	public AlleleCell() {
		setContentDisplay(ContentDisplay.RIGHT);
	}

	@Override
	protected void updateItem(final String allele, final boolean empty) {
		super.updateItem(allele, empty);
		if (empty || allele == null) {
			setText(null);
			setGraphic(null);
		} else {
			if (allele.length() > 5) {
				setText(allele.substring(0, 5));
				final Hyperlink more = new Hyperlink("(+" + (allele.length() - 5) + ")");
				final Hyperlink less = new Hyperlink("(-" + (allele.length() - 5) + ")");
				setGraphic(more);
				more.setOnAction(event -> {
					setText(allele);
					setGraphic(less);
				});
				less.setOnAction(event -> {
					setText(allele.substring(0, 5));
					setGraphic(more);
				});
			} else {
				setText(allele);
				setGraphic(null);
			}
		}
	}
}
