package org.uichuimi.variant.viewer.filter;

import htsjdk.samtools.util.Interval;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeType;
import htsjdk.variant.variantcontext.VariantContext;
import org.uichuimi.variant.viewer.components.Accessor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SampleFilter implements BaseFilter {

	private final List<String> samples;
	private final List<GenotypeType> types;
	private final Accessor accessor;
	private final int number;

	public SampleFilter(List<String> samples, List<GenotypeType> types, Accessor accessor, Integer number) {
		this.samples = samples;
		this.types = types;
		this.accessor = accessor;
		this.number = number == null ? 1 : number;
	}

	public boolean filter(VariantContext variantContext) {
		final List<GenotypeType> typeList = samples.stream()
			.map(variantContext::getGenotype)
			.map(Genotype::getType)
			.filter(types::contains)
			.collect(Collectors.toList());
		return switch (accessor) {
			case ALL -> typeList.size() == samples.size();
			case NONE -> typeList.size() == 0;
			case ANY -> typeList.size() >= number;
		};
	}

	public String display() {
		final StringBuilder result = new StringBuilder();
		result.append(accessor);
		if (number != 1) result.append(" ").append(number);
		result.append(" of ")
			.append(samples)
			.append(accessor == Accessor.ALL || number > 1 ? " are " : " is ")
			.append(types);
		return result.toString();
	}

	@Override
	public Collection<Interval> getInterval() {
		return null;
	}
}
