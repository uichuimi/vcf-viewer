package org.uichuimi.variant.viewer.components.filter;

import htsjdk.variant.variantcontext.VariantContext;
import org.uichuimi.variant.viewer.components.Accessor;

import java.util.List;

public class Filter {

	private final Field field;
	private final Accessor accessor;
	private final Operator operator;
	private final Object value;

	public Filter(final Field field, final Accessor accessor, final Operator operator, final Object value) {
		this.field = field;
		this.accessor = accessor;
		this.operator = operator;
		this.value = value;
	}

	public Field getField() {
		return field;
	}

	public Operator getOperator() {
		return operator;
	}

	public Object getValue() {
		return value;
	}

	public Accessor getAccessor() {
		return accessor;
	}

	private boolean filter(VariantContext variant) {
		final Object value = field.extract(variant);
		if (value == null) return false;
		if (field.isList()) {
			final List<Object> list = (List<Object>) value;
			return switch (accessor) {
				case ALL -> list.stream().allMatch(val -> operator.query(this.value, val));
				case NONE -> list.stream().noneMatch(val -> operator.query(this.value, val));
				default -> list.stream().anyMatch(val -> operator.query(this.value, val));
			};
		} else {
			return operator.query(this.value, value);
		}
	}
}
