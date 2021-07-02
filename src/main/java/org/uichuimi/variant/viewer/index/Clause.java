package org.uichuimi.variant.viewer.index;

import htsjdk.variant.variantcontext.GenotypeType;

import java.util.Set;

public class Clause {

	private final Set<String> samples;
	private final Set<GenotypeType> types;
	private final int minMatch;

	public Clause(Set<String> samples, Set<GenotypeType> types) {
		this(samples, types, samples.size());
	}

	public Clause(Set<String> samples, Set<GenotypeType> types, int minMatch) {
		this.samples = samples;
		this.types = types;
		this.minMatch = minMatch;
	}

	public Set<String> getSamples() {
		return samples;
	}

	public Set<GenotypeType> getTypes() {
		return types;
	}

	public int getMinMatch() {
		return minMatch;
	}

	@Override
	public String toString() {
		return "Clause{" + samples + " " + minMatch + " " + types + "}";
	}
}
