package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import org.uichuimi.variant.viewer.utils.Constants;

import java.util.Set;

public class FiltersCell extends TableCell<VariantContext, Set<String>> {

	private final SelectableLabel label = new SelectableLabel();

	public FiltersCell() {
		label.focusedProperty().addListener((obs, prev, focused) -> {
			if (focused) getTableView().getSelectionModel().select(getIndex());
		});
		label.setAlignment(Pos.CENTER);
	}

	@Override
	protected void updateItem(final Set<String> filters, final boolean empty) {
		super.updateItem(filters, empty);
		if (empty) {
			setGraphic(null);
		} else {
			if (filters == null || filters.isEmpty()) {
				label.setText(Constants.EMPTY_CELL);
			} else {
				label.setText(String.join(",", filters));
			}
			setGraphic(label);
		}
	}
}
