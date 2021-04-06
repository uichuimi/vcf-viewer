package org.uichuimi.variant.viewer.filter;

import htsjdk.variant.variantcontext.VariantContext;

import java.util.Collection;

public class PropertiesFilter implements VariantContextFilter {

	private final Collection<Filter> es;

	public PropertiesFilter(Collection<Filter> es) {
		this.es = es;
	}

	@Override
	public boolean filter(VariantContext variantContext) {
		return es.stream().allMatch(filter -> filter.filter(variantContext));
	}
}
