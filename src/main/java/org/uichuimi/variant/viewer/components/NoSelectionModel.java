package org.uichuimi.variant.viewer.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

/**
 *
 * @param <T> same as {@link MultipleSelectionModel}
 * @see <a href="https://stackoverflow.com/questions/20621752/javafx-make-listview-not-selectable-via-mouse" />
 */
public class NoSelectionModel<T> extends MultipleSelectionModel<T> {
	@Override
	public ObservableList<Integer> getSelectedIndices() {
		return FXCollections.emptyObservableList();
	}

	@Override
	public ObservableList<T> getSelectedItems() {
		return FXCollections.emptyObservableList();
	}

	@Override
	public void selectIndices(final int index, final int... indices) {

	}

	@Override
	public void selectAll() {

	}

	@Override
	public void selectFirst() {

	}

	@Override
	public void selectLast() {

	}

	@Override
	public void clearAndSelect(final int index) {

	}

	@Override
	public void select(final int index) {

	}

	@Override
	public void select(final T obj) {

	}

	@Override
	public void clearSelection(final int index) {

	}

	@Override
	public void clearSelection() {

	}

	@Override
	public boolean isSelected(final int index) {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public void selectPrevious() {

	}

	@Override
	public void selectNext() {

	}
}
