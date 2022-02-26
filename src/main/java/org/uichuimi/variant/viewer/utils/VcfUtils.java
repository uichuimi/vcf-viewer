package org.uichuimi.variant.viewer.utils;

import org.uichuimi.variant.viewer.database.FieldType;

import java.util.function.Function;

public class VcfUtils {


	private static final String NULL_VALUE = ".";
	private static final String ARRAY_SEPARATOR = ",";

	/**
	 * Returns an object or list of objects if multiple, parsing int and float values
	 *
	 * @param input    input object. If it is not a String it will be returned as is.
	 * @param type     target type, as indicated in the vcf file.
	 * @param multiple whether the field is an array (Number != 0 or 1)
	 *
	 * @return the parsed object or objects
	 */
	public static Object parse(Object input, FieldType type, boolean multiple) {
		Function<String, Object> parser = switch (type) {
			case Integer -> Integer::parseInt;
			case Float -> Float::parseFloat;
			case Boolean -> s -> true;
			case String -> s -> s;
		};
		if (input instanceof String) {
			String value = (String) input;
			// Handle special case
			if (value.equals(NULL_VALUE)) {
				return null;
			}
			if (multiple) {
				final String[] elements = value.split(ARRAY_SEPARATOR);
				final Object[] values = new Object[elements.length];
				boolean allNull = true;
				for (int i = 0; i < elements.length; i++) {
					if (!elements[i].equals(NULL_VALUE)) {
						values[i] = parser.apply(elements[i]);
						allNull = false;
					}
				}
				// Handle special case where input == ".,."
				if (allNull) {
					return null;
				}
				return values;
			} else return parser.apply(value);
		} else return input;
	}}
