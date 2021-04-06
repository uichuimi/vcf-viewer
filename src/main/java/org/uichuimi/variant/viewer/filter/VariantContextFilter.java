package org.uichuimi.variant.viewer.filter;

import htsjdk.variant.variantcontext.VariantContext;

public interface VariantContextFilter {

	boolean filter(VariantContext variantContext);
}
