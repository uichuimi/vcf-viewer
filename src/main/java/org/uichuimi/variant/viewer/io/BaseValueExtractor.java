package org.uichuimi.variant.viewer.io;

import htsjdk.variant.variantcontext.VariantContext;

public interface BaseValueExtractor {

	Object extract(VariantContext variant);
}
