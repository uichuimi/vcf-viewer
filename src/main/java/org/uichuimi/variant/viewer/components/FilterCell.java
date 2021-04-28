package org.uichuimi.variant.viewer.components;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.uichuimi.variant.viewer.filter.BaseFilter;

import static org.uichuimi.variant.viewer.components.PropertyFilters.FONT_AWESOME;

class FilterCell<F extends BaseFilter> extends ListCell<F> {
	private final Button delete = new Button(null, new Glyph(FONT_AWESOME, FontAwesome.Glyph.MINUS_CIRCLE));

	public FilterCell() {
		delete.setFocusTraversable(false);
		delete.getStyleClass().add("icon-button");
		delete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
	}

	@Override
	protected void updateItem(final F filter, final boolean empty) {
		super.updateItem(filter, empty);
		setText(null);
		if (empty || filter == null) {
			setGraphic(null);
		} else {
			final BorderPane content = new BorderPane();
			content.setLeft(new BorderPane(new SelectableLabel(filter.display())));
			content.setRight(delete);
			delete.setOnAction(event -> getListView().getItems().remove(filter));
			setGraphic(content);
		}
	}
}
