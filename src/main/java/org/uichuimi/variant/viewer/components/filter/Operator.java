package org.uichuimi.variant.viewer.components.filter;

import java.util.function.BiFunction;

public enum Operator {
	INT_LOWER((a, b) -> (int) a < (int) b, "<"),
	INT_LOWER_OR_EQUAL((a, b) -> (int) a <= (int) b, "<="),
	INT_EQUAL((a, b) -> (int) a == (int) b, "="),
	INT_GREATER_OR_EQUAL((a, b) -> (int) a >= (int) b, ">="),
	INT_GREATER((a, b) -> (int) a > (int) b, ">"),

	FLOAT_LOWER((a, b) -> (float) a < (float) b, "<"),
	FLOAT_LOWER_OR_EQUAL((a, b) -> (float) a <= (float) b, "<="),
	FLOAT_EQUAL((a, b) -> (float) a == (float) b, "="),
	FLOAT_GREATER_OR_EQUAL((a, b) -> (float) a >= (float) b, ">="),
	FLOAT_GREATER((a, b) -> (float) a > (float) b, ">"),

	FLAG_PRESENT((a, b) -> (boolean) a, "present"),
	FLAG_NOT_PRESENT((a, b) -> a == null || !(boolean) a, "not present"),

	TEXT_EQUAL((a, b) -> ((String) a).toLowerCase().equals(b), "equals"),
	TEXT_NOT_EQUAL((a, b) -> !a.equals(b), "equals"),
	TEXT_CONTAINS((a, b) -> ((String) a).toLowerCase().contains((String) b), "contains");

	private final BiFunction<Object, Object, Boolean> operation;
	private final String display;

	Operator(final BiFunction<Object, Object, Boolean> operation, final String display) {
		this.operation = operation;
		this.display = display;
	}

	public Boolean query(Object a, Object b) {
		return a != null && operation.apply(a, b);
	}

	public String getDisplay() {
		return display;
	}
}
