package org.uichuimi.variant.viewer.io;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.uichuimi.variant.viewer.filter.VariantContextFilter;

import java.io.File;
import java.util.Collection;

/**
 * Reads and writes a stream of {@link VariantContext}s. Reading is done via input file. Task result is an observable
 * list, which will contain a maximum of maxResults variants. If an output file is specified, then all filtered variants
 * are saved to output, even if they are more than maxResults.
 */
public class VariantContextPipe extends Task<ObservableList<VariantContext>> {

	private final ObservableList<VariantContext> variants = FXCollections.observableArrayList();
	private final LongProperty lines = new SimpleLongProperty(0);
	private final LongProperty filtered = new SimpleLongProperty(0);
	private final Collection<VariantContextFilter> filters;
	private final File input;
	private final File output;
	private final int maxResults;
	private final Long total;

	/**
	 * Creates a variant context pipe, writing to output only filtered variants. A
	 *
	 * @param input      input file
	 * @param output     output file. May be null
	 * @param filters    applied to every variant
	 * @param maxResults max number of variants in returned list
	 * @param total      known size of input file. This can be set for progress purposes.
	 */
	public VariantContextPipe(final File input, final File output, final Collection<VariantContextFilter> filters, Integer maxResults, Long total) {
		this.input = input;
		this.output = output;
		this.filters = filters;
		this.maxResults = maxResults == null ? 50 : maxResults;
		this.total = total;
	}

	@Override
	protected ObservableList<VariantContext> call() throws Exception {
		final VCFFileReader reader = new VCFFileReader(input, false);
		final VariantContextWriter writer = output == null
			? null
			: new VariantContextWriterBuilder().setReferenceDictionary(reader.getHeader().getSequenceDictionary()).setOutputFile(output).build();

		try (reader; writer) {
			if (writer != null) writer.writeHeader(reader.getHeader());

			for (final VariantContext variant : reader) {
				if (isCancelled()) break;
				if (filters.stream().allMatch(filter -> filter.filter(variant))) {
					filtered.set(filtered.get() + 1);
					if (filtered.get() <= maxResults) {
						variants.add(variant);
					}
					if (writer != null) writer.add(variant);
				}
				lines.set(lines.get() + 1);
				if (lines.get() % 1000 == 0) {
					updateMessage("%s:%,d".formatted(variant.getContig(), variant.getStart()));
					if (total != null) {
						updateProgress(lines.get(), total);
					}
				}
			}
		}
		return variants;
	}

	public LongProperty linesReadProperty() {
		return lines;
	}

	public LongProperty filteredProperty() {
		return filtered;
	}

	public ObservableList<VariantContext> getVariants() {
		return variants;
	}
}
