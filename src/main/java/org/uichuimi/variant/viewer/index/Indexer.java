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
import org.uichuimi.variant.viewer.components.filter.Field;
import org.uichuimi.variant.viewer.components.filter.FieldBuilder;
import org.uichuimi.variant.viewer.utils.Chromosome;
import org.uichuimi.variant.viewer.utils.Constants;
import org.uichuimi.variant.viewer.utils.GenomeProgress;

import java.io.*;
import java.util.*;

public class Indexer extends Task<VcfIndex> {

	private static final int LIMIT = 75;
	private final File file;

	public Indexer(final File file) {
		this.file = file;
	}

	@Override
	protected VcfIndex call() throws Exception {
		maybeTabix();
		final File indexFile = new File(file.getAbsolutePath() + ".vcf-index");
		if (indexFile.exists()) {
			try (FileInputStream in = new FileInputStream(indexFile)) {
				return (VcfIndex) new ObjectInputStream(in).readObject();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		final VcfIndex index = createIndex();
		try (FileOutputStream out = new FileOutputStream(indexFile)) {
			new ObjectOutputStream(out).writeObject(index);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return index;
	}

	private VcfIndex createIndex() throws Exception {
		final Map<String, Set<String>> options = new TreeMap<>();
		final Set<String> contigs = new LinkedHashSet<>();
		final Set<String> filters = new LinkedHashSet<>();
		long lineCount = 0;
		final VCFHeader header;
		String zipFile = file.getAbsolutePath() + ".gtbit";
		try (VCFFileReader reader = new VCFFileReader(file, false);
		     GtBitsetArchiveBuilder gtBitsetArchiveBuilder = new GtBitsetArchiveBuilder(new File(zipFile), reader.getHeader())) {
			header = reader.getHeader();
			final List<String> people = header.getGenotypeSamples();
			//  4 -> number of possible genotypes
			// 64 -> number of bits in a long
			final Chromosome.Namespace namespace = Chromosome.Namespace.guess(reader.getHeader());
			reader.getHeader().getInfoHeaderLines().stream()
				.filter(line -> line.getType() == VCFHeaderLineType.String)
				.forEach(line -> options.put(line.getID(), new TreeSet<>()));
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
				gtBitsetArchiveBuilder.addSite(variant);
				if (lineCount++ % 1000 == 0) {
					updateProgress(GenomeProgress.getProgress(variant, namespace), 1);
					updateMessage("Indexing " + variant.getContig() + " : " + variant.getStart());
				}
			}
			final List<Field> fields = new ArrayList<>(
				List.of(chromField(new ArrayList<>(contigs)),
					posField(), qualField(), idField(),
					filterField(new ArrayList<>(filters)))
			);
			for (final VCFInfoHeaderLine line : header.getInfoHeaderLines()) {
				fields.add(toField(line, options.getOrDefault(line.getID(), Set.of())));
			}
			return new VcfIndex(fields, lineCount, gtBitsetArchiveBuilder.getArchive());
		}
	}

	private void maybeTabix() throws IOException {
		boolean needsIndex = true;
		try (VCFFileReader reader = new VCFFileReader(file, true)) {
			needsIndex = false;
		} catch (TribbleException ignored) {
		}
		if (needsIndex) {
			updateMessage("Creating tabix index");
			long pos = 0;
			try (VCFFileReader reader = new VCFFileReader(file, false)) {
				final TabixIndexCreator tabixIndexCreator = new TabixIndexCreator(TabixFormat.VCF);
				tabixIndexCreator.setIndexSequenceDictionary(reader.getHeader().getSequenceDictionary());
				int i = 0;
				for (final VariantContext variant : reader) {
					tabixIndexCreator.addFeature(variant, pos++);
					if (i++ % 1000 == 0)
						updateMessage("Creating tabix index (%s:%,d)".formatted(variant.getContig(), variant.getStart()));
				}
				final Index index = tabixIndexCreator.finalizeIndex(pos);
				index.writeBasedOnFeatureFile(file);
			}
		}
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
