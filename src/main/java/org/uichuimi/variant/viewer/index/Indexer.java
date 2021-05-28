package org.uichuimi.variant.viewer.index;

import htsjdk.tribble.TribbleException;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import javafx.concurrent.Task;
import org.uichuimi.variant.viewer.filter.Field;
import org.uichuimi.variant.viewer.filter.FieldBuilder;
import org.uichuimi.variant.viewer.utils.Chromosome;
import org.uichuimi.variant.viewer.utils.Constants;
import org.uichuimi.variant.viewer.utils.GenomeProgress;
import org.uichuimi.variant.viewer.utils.ResourceConsumer;

import java.io.*;
import java.util.*;

public class Indexer extends Task<VcfIndex> {

	private static final int LIMIT = 75;
	private final File file;

	public Indexer(final File file) {
		this.file = file;
	}

	@Override
	protected VcfIndex call() {
		final File indexFile = new File(file.getAbsolutePath() + ".vcf-index");
		if (indexFile.exists()) {
			updateMessage("Reading index");
			try (FileInputStream in = new FileInputStream(indexFile)) {
				return (VcfIndex) new ObjectInputStream(in).readObject();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		final VcfIndex index = createIndex();
		try (FileOutputStream out = new FileOutputStream(indexFile)) {
			updateMessage("Saving index");
			new ObjectOutputStream(out).writeObject(index);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return index;
	}

	private VcfIndex createIndex() {
		final Collection<ResourceConsumer<VCFHeader, VariantContext>> consumers = new ArrayList<>();
		final ViewerIndexCreator indexCreator = new ViewerIndexCreator();
		consumers.add(indexCreator);
		// Optionally, create TBI
		if (needsIndex()) {
			consumers.add(new TabixCreator(file));
		}
		long lineCount = 0;
		try (VCFFileReader reader = new VCFFileReader(file, false)) {
			final VCFHeader header = reader.getHeader();
			for (ResourceConsumer<VCFHeader, VariantContext> consumer : consumers) {
				consumer.start(header);
			}
			final Chromosome.Namespace namespace = Chromosome.Namespace.guess(reader.getHeader());
			for (final VariantContext variant : reader) {
				for (ResourceConsumer<VCFHeader, VariantContext> consumer : consumers) {
					consumer.consume(variant, lineCount);
				}
				if (lineCount++ % 1000 == 0) {
					updateProgress(GenomeProgress.getProgress(variant, namespace), 1);
					updateMessage("Indexing " + variant.getContig() + " : " + variant.getStart());
				}
			}
			for (ResourceConsumer<VCFHeader, VariantContext> consumer : consumers) {
				consumer.finnish(lineCount);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return indexCreator.get();
	}

	private boolean needsIndex() {
		boolean needsIndex = true;
		try (VCFFileReader ignored = new VCFFileReader(file)) {
			needsIndex = false;
		} catch (TribbleException ignored) {
		}
		return needsIndex;
	}

	private static class ViewerIndexCreator implements ResourceConsumer<VCFHeader, VariantContext> {

		private final Map<String, Set<String>> options = new TreeMap<>();
		private final Set<String> contigs = new LinkedHashSet<>();
		private final Set<String> filters = new LinkedHashSet<>();
		private Chromosome.Namespace namespace;
		private VCFHeader header;
		private VcfIndex index;

		@Override
		public void start(VCFHeader header) {
			this.header = header;
			namespace = Chromosome.Namespace.guess(header);
			header.getInfoHeaderLines().stream()
				.filter(line -> line.getType() == VCFHeaderLineType.String)
				.forEach(line -> options.put(line.getID(), new TreeSet<>()));

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

		}

		@Override
		public void finnish(long position) throws Exception {
			final List<Field> fields = new ArrayList<>(
				List.of(chromField(new ArrayList<>(contigs)),
					posField(), qualField(), idField(),
					filterField(new ArrayList<>(filters)))
			);
			for (final VCFInfoHeaderLine line : header.getInfoHeaderLines()) {
				fields.add(toField(line, options.getOrDefault(line.getID(), Set.of())));
			}
			index = new VcfIndex(fields, position);
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

	private static class TabixCreator implements ResourceConsumer<VCFHeader, VariantContext> {

		private final TabixIndexCreator tabixIndexCreator = new TabixIndexCreator(TabixFormat.VCF);
		private final File vcfFile;

		private TabixCreator(File vcfFile) {this.vcfFile = vcfFile;}

		@Override
		public void start(VCFHeader vcfHeader) {
			tabixIndexCreator.setIndexSequenceDictionary(vcfHeader.getSequenceDictionary());
		}

		@Override
		public void consume(VariantContext variantContext, long position) {
			tabixIndexCreator.addFeature(variantContext, position);
		}

		@Override
		public void finnish(long position) throws IOException {
			final Index index = tabixIndexCreator.finalizeIndex(position);
			index.writeBasedOnFeatureFile(vcfFile);
		}

	}
}
