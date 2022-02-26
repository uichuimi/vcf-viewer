package org.uichuimi.variant.viewer.database;

public class Field {
	private final String name;
	private final FieldType type;

	public Field(String name, FieldType type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String toString() {
		return getName() + ":" + getType();
	}

	public String getName() {
		return name;
	}

	public FieldType getType() {
		return type;
	}
}
