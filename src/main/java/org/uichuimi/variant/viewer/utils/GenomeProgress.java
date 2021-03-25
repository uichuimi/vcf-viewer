package org.uichuimi.variant.viewer.utils;

import htsjdk.variant.variantcontext.VariantContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple utility to know the approximate progress in the genome based on a coordinate.
 */
public class GenomeProgress {

	private final static List<Chromosome> CHROMOSOMES = ChromosomeFactory.getChromosomeList();
	private final static Map<Chromosome, Long> abs = new HashMap<>();
	private final static Long total;

	static {
		long t = 0L;
		for (Chromosome chromosome : CHROMOSOMES) {
			abs.put(chromosome, t);
			t += chromosome.getLength();
		}
		total = t;
	}
	
	public static double getProgress(VariantContext variant) {
		final Long base = abs.get(Chromosome.get(variant.getContig()));
		if (base == null) return 0.99;
		return (base + variant.getStart()) / ( double) total;
	}

	public static double getProgress(VariantContext variant, Chromosome.Namespace namespace) {
		final Long base = abs.get(Chromosome.get(variant.getContig(), namespace));
		if (base == null) return 0.99;
		return (base + variant.getStart()) / ( double) total;
	}

}
