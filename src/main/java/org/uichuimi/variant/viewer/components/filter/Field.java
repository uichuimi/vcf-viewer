package org.uichuimi.variant.viewer.components.filter;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFConstants;
import org.uichuimi.variant.viewer.utils.Constants;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Field implements Serializable {

	@Serial
	private final static long serialVersionUID = 9220147668706248571L;
	private final boolean list;
	private final List<String> options;
	private final Category category;
	private final String name;
	private final Type type;
	private transient Function<VariantContext, Object> extractor;

	public Field(final Type type, final List<String> options, final String name, final boolean list, final Category category) {
		this.type = type;
		this.options = options;
		this.category = category;
		this.name = name;
		this.list = list;
		this.extractor = createExtractorMain();
	}

	private Function<VariantContext, Object> createExtractorMain() {
		final Function<VariantContext, Object> extractor = createExtractor();
		return variantContext -> {
			final Object attribute = variantContext.getAttribute(name);
			if (attribute == null || attribute.equals(VCFConstants.EMPTY_ID_FIELD)) {
				return null;
			} else {
				return extractor.apply(variantContext);
			}
		};
	}

	private Function<VariantContext, Object> createExtractor() {
		if (category == Category.INFO) {
			// VariantContext#getAttribute(String) returns a String, we must intelligently call a parser method
			if (list) {
				return switch (type) {
					case FLOAT -> variant -> variant.getAttributeAsDoubleList(name, 0.0);
					case INTEGER -> variant -> variant.getAttributeAsIntList(name, 0);
					default -> variant -> variant.getAttributeAsStringList(name, null);
				};
			} else {
				return switch (type) {
					case FLOAT -> variant -> variant.getAttributeAsDouble(name, 0.0);
					case INTEGER -> variant -> variant.getAttributeAsInt(name, 0);
					default -> variant -> variant.getAttributeAsString(name, null);
				};
			}
		} else if (category == Category.STANDARD) {
			return switch (name) {
				case Constants.CHROM -> VariantContext::getContig;
				case Constants.POS -> VariantContext::getStart;
				case Constants.ID -> VariantContext::getID;
				case Constants.REF -> variant -> variant.getReference().getBaseString();
				case Constants.ALT -> variant -> variant.getAlternateAlleles().stream().map(Allele::getBaseString).collect(Collectors.joining(","));
				case Constants.QUAL -> VariantContext::getPhredScaledQual;
				case Constants.FILTER -> VariantContext::getFilters;
				default -> throw new IllegalArgumentException("No standard column " + name);
			};
		} else if (category == Category.FORMAT) {
			throw new UnsupportedOperationException("Category field not supported");
		} else throw new IllegalArgumentException("Unknown category " + category);
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

	public String getName() {
		return name;
	}

	public Category getCategory() {
		return category;
	}

	public boolean isList() {
		return list;
	}

	@Override
	public String toString() {
		return "Field{" +
			"list=" + list +
			", options=" + options +
			", displayName='" + name + '\'' +
			", type=" + type +
			'}';
	}

	@Serial
	private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException, IOException {
		// perform the default de-serialization first
		inputStream.defaultReadObject();
		extractor = createExtractorMain();
	}

	public enum Category {
		STANDARD, INFO, FORMAT
	}

	public enum Type {
		TEXT(List.of(Operator.TEXT_CONTAINS, Operator.TEXT_EQUAL, Operator.TEXT_NOT_EQUAL)),
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
