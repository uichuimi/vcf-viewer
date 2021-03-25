package org.uichuimi.variant.viewer.components.filter;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFInfoHeaderLine;

import java.util.List;
import java.util.function.Function;

public class FieldBuilder {

	private FieldBuilder() {
	}

	public static Field create(final VCFInfoHeaderLine line) {
		final Field.Type type = switch (line.getType()) {
			case Flag -> Field.Type.FLAG;
			case Float -> Field.Type.FLOAT;
			case Integer -> Field.Type.INTEGER;
			case String, Character -> Field.Type.TEXT;
		};
		// Options cannot be known from header line, they must be computed by scanning the file
		final List<String> options = List.of();
		final String name = line.getID();
		final boolean list = line.getCountType() != VCFHeaderLineCount.INTEGER || line.getCount() > 1;

		// VariantContext#getAttribute(String) returns a String, we must intelligently call a parser method
		final Function<VariantContext, Object> extractor;
		if (list) {
			extractor = switch (type) {
				case FLOAT -> variant -> variant.getAttributeAsDoubleList(name, 0.0);
				case INTEGER -> variant -> variant.getAttributeAsIntList(name, 0);
				default -> variant -> variant.getAttributeAsStringList(name, null);
			};
		} else {
			extractor = switch (type) {
				case FLOAT -> variant -> variant.getAttributeAsDouble(name, 0.0);
				case INTEGER -> variant -> variant.getAttributeAsInt(name, 0);
				default -> variant -> variant.getAttributeAsString(name, null);
			};
		}

		return new Field(type, options, extractor, name, list);
	}
}
