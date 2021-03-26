package org.uichuimi.variant.viewer.components;

import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import javafx.concurrent.Task;
import org.uichuimi.variant.VcfIndex;
import org.uichuimi.variant.viewer.components.filter.Field;
import org.uichuimi.variant.viewer.components.filter.FieldBuilder;
import org.uichuimi.variant.viewer.utils.Chromosome;
import org.uichuimi.variant.viewer.utils.GenomeProgress;

import java.io.File;
import java.io.IOException;
import java.util.*;

class Indexer extends Task<VcfIndex> {

	private static final int LIMIT = 75;
	private final File file;

	public Indexer(final File file) {
		this.file = file;
	}

	@Override
	protected VcfIndex call() throws Exception {
		final Map<String, Set<String>> options = new TreeMap<>();
		final Set<String> contigs = new LinkedHashSet<>();
		final Set<String> filters = new LinkedHashSet<>();
		maybeTabix();
		VCFHeader header = null;
		try (VCFFileReader reader = new VCFFileReader(file, false)) {
			header = reader.getHeader();
			final Chromosome.Namespace namespace = Chromosome.Namespace.guess(reader.getHeader());
			reader.getHeader().getInfoHeaderLines().stream()
				.filter(line -> line.getType() == VCFHeaderLineType.String)
				.forEach(line -> options.put(line.getID(), new TreeSet<>()));
			int line = 0;
			for (final VariantContext variant : reader) {
				// Add new options
				contigs.add(variant.getContig());
				filters.addAll(variant.getFilters());
				for (final String id : options.keySet()) {
					final Set<String> set = options.get(id);
					final Collection<String> value = variant.getCommonInfo().getAttributeAsStringList(id, null);
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
				if (line++ % 1000 == 0) {
					updateProgress(GenomeProgress.getProgress(variant, namespace), 1);
					updateMessage("Indexing " + variant.getContig() + " : " + variant.getStart());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		final List<Field> fields = new ArrayList<>(
			List.of(chromField(new ArrayList<>(contigs)),
				posField(), qualField(), idField(),
				filterField(new ArrayList<>(filters)))
		);
		for (final VCFInfoHeaderLine line : header.getInfoHeaderLines()) {
			fields.add(toField(line, options.getOrDefault(line.getID(), Set.of())));
		}
		for (final Field field : fields) {
			System.out.println(field);
		}

		return new VcfIndex(options, fields);
	}

	private void maybeTabix() {
		try (VCFFileReader reader = new VCFFileReader(file, false)) {
			if (!reader.isQueryable()) {
				System.out.println("File needs index");
				final TabixIndexCreator tabixIndexCreator = new TabixIndexCreator(TabixFormat.VCF);
				long pos = 0;
				tabixIndexCreator.setIndexSequenceDictionary(reader.getHeader().getSequenceDictionary());
				for (final VariantContext variantContext : reader) {
					tabixIndexCreator.addFeature(variantContext, pos++);
				}
				final Index index = tabixIndexCreator.finalizeIndex(pos);
				index.writeBasedOnFeatureFile(file);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Field chromField(List<String> contigs) {
		return new Field(Field.Type.TEXT, contigs, VariantContext::getContig, "Chromosome", false);
	}

	private Field posField() {
		return new Field(Field.Type.INTEGER, null, VariantContext::getStart, "Position", false);
	}

	private Field qualField() {
		return new Field(Field.Type.FLOAT, null, VariantContext::getPhredScaledQual, "Quality", false);
	}

	private Field idField() {
		return new Field(Field.Type.TEXT, null, VariantContext::getID, "Identifier", false);
	}

	private Field filterField(final List<String> filters) {
		return new Field(Field.Type.TEXT, filters, VariantContext::getFilters, "Filter", true);
	}

	private Field toField(final VCFInfoHeaderLine line, final Set<String> options) {
		return FieldBuilder.create(line, new ArrayList<>(options));
	}
}
