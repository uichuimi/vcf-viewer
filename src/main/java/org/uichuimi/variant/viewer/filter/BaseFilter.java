package org.uichuimi.variant.viewer.filter;

import htsjdk.samtools.util.Interval;
import htsjdk.variant.variantcontext.VariantContext;

import java.util.Collection;

public interface BaseFilter {

	String display();

	Collection<Interval> getInterval();

	boolean filter(VariantContext variant);
}
