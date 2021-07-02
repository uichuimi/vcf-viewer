package org.uichuimi.variant.viewer.index;

import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeType;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import org.uichuimi.variant.viewer.filter.Field;
import org.uichuimi.variant.viewer.filter.FieldBuilder;
import org.uichuimi.variant.viewer.utils.Chromosome;
import org.uichuimi.variant.viewer.utils.Constants;
import org.uichuimi.variant.viewer.utils.ResourceConsumer;

import java.util.*;

class ViewerIndexCreator implements ResourceConsumer<VCFHeader, VariantContext> {

	private static final int LIMIT = 75;
	private static final int NUMBER_OF_TYPES = GenotypeType.values().length;
	private final Map<String, Set<String>> options = new TreeMap<>();
	private final Set<String> contigs = new LinkedHashSet<>();
	private final Set<String> filters = new LinkedHashSet<>();
	private final List<long[]> bitSets = new ArrayList<>();
	private Chromosome.Namespace namespace;
	private VCFHeader header;
	private VcfIndex index;
	private int WORDS_PER_SITE;


	@Override
	public void start(VCFHeader header) {
		this.header = header;
		namespace = Chromosome.Namespace.guess(header);
		header.getInfoHeaderLines().stream()
			.filter(line -> line.getType() == VCFHeaderLineType.String)
			.forEach(line -> options.put(line.getID(), new TreeSet<>()));
		WORDS_PER_SITE = (int) Math.ceil(header.getNGenotypeSamples()  / 64. * NUMBER_OF_TYPES);
	}

	@Override
	public void consume(VariantContext variantContext, long position) {
		contigs.add(variantContext.getContig());
		filters.addAll(variantContext.getFilters());
		for (final String id : options.keySet()) {
			final Set<String> set = options.get(id);
			final Collection<String> value = variantContext.getCommonInfo().getAttributeAsStringList(id, null);
			if (value == null) continue;
			for (String val : value) {
				if (val != null) {
					set.add(val);
				}
			}
		}
		// Check too long options
		for (final String id : new ArrayList<>(options.keySet())) {
			if (options.get(id).size() > LIMIT) {
				options.remove(id);
			}
		}
		final long[] bits = new long[WORDS_PER_SITE];
		// Add samples to genotype search
		for (int i = 0; i < header.getNGenotypeSamples(); i++) {
			final Genotype genotype = variantContext.getGenotype(i);
			BitUtils.set(bits, NUMBER_OF_TYPES * i + genotype.getType().ordinal());
		}
		this.bitSets.add(bits);

	}

	@Override
	public void finnish(long position) {
		final List<Field> fields = new ArrayList<>(
			List.of(chromField(new ArrayList<>(contigs)),
				posField(), qualField(), idField(),
				filterField(new ArrayList<>(filters)))
		);
		for (final VCFInfoHeaderLine line : header.getInfoHeaderLines()) {
			fields.add(toField(line, options.getOrDefault(line.getID(), Set.of())));
		}
		final long[] data = new long[WORDS_PER_SITE * bitSets.size()];
		for (int w = 0; w < bitSets.size(); w++) {
			System.arraycopy(bitSets.get(w), 0, data, w * WORDS_PER_SITE, WORDS_PER_SITE);
		}
		final GenotypeSearchEngine searchEngine = new GenotypeSearchEngine(data, header.getGenotypeSamples(), WORDS_PER_SITE);
		index = new VcfIndex(fields, position, searchEngine);
	}

	VcfIndex get() {
		return index;
	}

	private Field chromField(List<String> contigs) {
		return new Field(Field.Type.TEXT, contigs, Constants.CHROM, false, Field.Category.STANDARD);
	}

	private Field posField() {
		return new Field(Field.Type.INTEGER, List.of(), Constants.POS, false, Field.Category.STANDARD);
	}

	private Field qualField() {
		return new Field(Field.Type.FLOAT, List.of(), Constants.QUAL, false, Field.Category.STANDARD);
	}

	private Field idField() {
		return new Field(Field.Type.TEXT, List.of(), Constants.ID, false, Field.Category.STANDARD);
	}

	private Field filterField(final List<String> filters) {
		return new Field(Field.Type.TEXT, filters, Constants.FILTER, true, Field.Category.STANDARD);
	}

	private Field toField(final VCFInfoHeaderLine line, final Set<String> options) {
		return FieldBuilder.create(line, new ArrayList<>(options));
	}

}
