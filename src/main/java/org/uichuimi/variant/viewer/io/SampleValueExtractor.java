package org.uichuimi.variant.viewer.io;

import htsjdk.variant.variantcontext.Genotype;

public interface SampleValueExtractor {

	Object extract(Genotype genotype);
}
