package org.uichuimi.variant.viewer.components;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;

/**
 * A cell that displays text that can be selected, but not modified. Can not render text and graphics.
 * @param <S>
 * @param <T>
 */
public class NaturalCell<S, T> extends TableCell<S, T> {

	/**
	 * Creates a new NaturalCell, which replaces the cell with a non-editable TextField.
	 */
	public NaturalCell() {
		setEditable(true);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		final SelectableLabel label = new SelectableLabel();
		textProperty().bindBidirectional(label.textProperty());
		setGraphic(label);
		textAlignmentProperty().bindBidirectional(label.textAlignmentProperty());
		alignmentProperty().bindBidirectional(label.alignmentProperty());
	}

	@Override
	protected void updateItem(final T item, final boolean empty) {
		super.updateItem(item, empty);
		setText(item == null || empty ? null : String.valueOf(item));
	}
}
