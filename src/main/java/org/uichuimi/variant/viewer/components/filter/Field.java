package org.uichuimi.variant.viewer.components.filter;

import htsjdk.variant.variantcontext.VariantContext;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Field {

	private final Function<VariantContext, Object> extractor;
	private final boolean list;
	private final List<String> options;
	private final String displayName;
	private final Type type;

	public Field(final Type type, final List<String> options, final Function<VariantContext, Object> extractor, final String displayName, final boolean list) {
		this.type = type;
		this.options = options;
		this.extractor = extractor;
		this.displayName = displayName;
		this.list = list;
	}

	public Collection<Operator> getOperators() {
		return type.getOperators();
	}

	public Type getType() {
		return type;
	}

	public List<String> getOptions() {
		return options;
	}

	public Object extract(final VariantContext variant) {
		return extractor.apply(variant);
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isList() {
		return list;
	}

	@Override
	public String toString() {
		return "Field{" +
			"list=" + list +
			", options=" + options +
			", displayName='" + displayName + '\'' +
			", type=" + type +
			'}';
	}

	public enum Type {
		TEXT(List.of(Operator.TEXT_CONTAINS, Operator.TEXT_EQUAL, Operator.TEXT_NOT_EQUAL)),
		MULTIPLE(List.of(Operator.TEXT_EQUAL)),
		FLOAT(List.of(Operator.FLOAT_LOWER, Operator.FLOAT_LOWER_OR_EQUAL, Operator.FLOAT_EQUAL, Operator.FLOAT_GREATER_OR_EQUAL, Operator.FLOAT_GREATER)),
		INTEGER(List.of(Operator.INT_LOWER, Operator.INT_LOWER_OR_EQUAL, Operator.INT_EQUAL, Operator.INT_GREATER_OR_EQUAL, Operator.INT_GREATER)),
		FLAG(List.of(Operator.FLAG_PRESENT, Operator.FLAG_NOT_PRESENT));

		private final Collection<Operator> operators;

		Type(final Collection<Operator> operators) {
			this.operators = operators;
		}

		public Collection<Operator> getOperators() {
			return operators;
		}
	}
}
