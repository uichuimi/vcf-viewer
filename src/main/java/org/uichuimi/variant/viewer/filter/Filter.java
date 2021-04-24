package org.uichuimi.variant.viewer.filter;

import htsjdk.variant.variantcontext.VariantContext;
import org.uichuimi.variant.viewer.components.Accessor;

import java.util.Collection;

public class Filter {
	/*
	 * field		| accessor	| operator		| value
	 * ------------ | --------- | ------------- | ----------
	 * chrom		| null		| null			| chr1,chr2
	 * pos			| null		| <,<=,=,>=,> 	| 1000
	 * filter		| any		| null			| options
	 *
	 *
	 * Filter
	 * 	- field : Field
	 * 		- extractor : variant -> value
	 * 		- list : boolean
	 * 		- options : [String]
	 * 		- name : String
	 * 		- type : Type
	 * 			- operators : [Operator]
	 * 	- operator : Operator
	 * 		- query() : boolean
	 * 	- accessor : Accessor
	 * 	- value : Object|[Object]
	 *
	 */

	private final Field field;
	private final Accessor accessor;
	private final Operator operator;
	private final Object value;
	private final boolean strict;

	public Filter(final Field field, final Accessor accessor, final Operator operator, final Object value, boolean strict) {
		this.field = field;
		this.accessor = accessor;
		this.operator = operator;
		this.value = value;
		this.strict = strict;
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

	public boolean isStrict() {
		return strict;
	}

	public boolean filter(VariantContext variant) {
		final Object value = field.extract(variant);
		if (value == null) return !strict;
		return filter(this.value, value);
	}

	private boolean filter(Object thisValue, Object queryValue) {
		if (thisValue instanceof Collection) {
			return filter((Collection<?>) thisValue, queryValue);
		}
		if (queryValue instanceof Collection) {
			return filter(thisValue, (Collection<?>) queryValue);
		}
		return operator.query(queryValue, thisValue);
	}

	private boolean filter(final Object thisValue, final Collection<?> queryValue) {
		return switch (accessor) {
			case ALL -> queryValue.stream().allMatch(val -> filter(thisValue, val));
			case NONE -> queryValue.stream().noneMatch(val -> filter(thisValue, val));
			default -> queryValue.stream().anyMatch(val -> filter(thisValue, val));
		};
	}

	private boolean filter(Collection<?> thisValue, Object queryValue) {
		return thisValue.stream().anyMatch(val -> filter(val, queryValue));
	}

	@Override
	public String toString() {
		return "Filter{" +
			"field=" + field +
			", accessor=" + accessor +
			", operator=" + operator +
			", value=" + value +
			", strict=" + strict +
			'}';
	}

	public String display() {
		final StringBuilder builder = new StringBuilder();
		if (field.isList()) builder.append(accessor).append(" ");
		builder.append(field.getName());
		builder.append(" ").append(operator.getDisplay());
		if (value != null) {
			builder.append(" ").append(value);
		}
		return builder.toString();
	}
}
