package org.uichuimi.variant.viewer.components.filter;

import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFInfoHeaderLine;

import java.util.List;

public class FieldBuilder {

	private FieldBuilder() {
	}

	public static Field create(final VCFInfoHeaderLine line) {
		return create(line, List.of());
	}

	public static Field create(final VCFInfoHeaderLine line, final List<String> options) {
		final Field.Type type = switch (line.getType()) {
			case Flag -> Field.Type.FLAG;
			case Float -> Field.Type.FLOAT;
			case Integer -> Field.Type.INTEGER;
			case String, Character -> Field.Type.TEXT;
		};
		// Options cannot be known from header line, they must be computed by scanning the file
		final String name = line.getID();
		final boolean list = line.getCountType() != VCFHeaderLineCount.INTEGER || line.getCount() > 1;

		return new Field(type, options, name, list, Field.Category.INFO);
	}
}
