package org.uichuimi.variant.viewer.io;

import htsjdk.samtools.util.Interval;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.uichuimi.variant.viewer.components.MainView;
import org.uichuimi.variant.viewer.filter.BaseFilter;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Reads and writes a stream of {@link VariantContext}s. Reading is done via input file. Task result is an observable
 * list, which will contain a maximum of maxResults variants. If an output file is specified, then all filtered variants
 * are saved to output, even if they are more than maxResults.
 */
public class VariantContextPipe extends Task<ObservableList<VariantContext>> {

	private final ObservableList<VariantContext> variants = FXCollections.observableArrayList();
	private final LongProperty lines = new SimpleLongProperty(0);
	private final LongProperty filtered = new SimpleLongProperty(0);
	private final List<BaseFilter> filters;
	private final File input;
	private final File output;
	private final int maxResults;
	private final Long total;

	/**
	 * Creates a variant context pipe, writing to output only filtered variants. A
	 *  @param input      input file
	 * @param output     output file. May be null
	 * @param filters    applied to every variant
	 * @param maxResults max number of variants in returned list
	 * @param total      known size of input file. This can be set for progress purposes.
	 */
	public VariantContextPipe(final File input, final File output, final List<BaseFilter> filters, Integer maxResults, Long total) {
		this.input = input;
		this.output = output;
		this.filters = filters;
		this.maxResults = maxResults == null ? 50 : maxResults;
		this.total = total;
	}

	@Override
	protected ObservableList<VariantContext> call() {
		final VCFFileReader reader = new VCFFileReader(input);
		final VariantContextWriter writer = output == null
			? null
			: new VariantContextWriterBuilder().setReferenceDictionary(reader.getHeader().getSequenceDictionary()).setOutputFile(output).build();

		final Collection<Interval> intervals = new TreeSet<>();
		for (BaseFilter filter : filters) {
			if (filter.getInterval() != null) {
				intervals.addAll(filter.getInterval());
			}
		}

		try (reader; writer) {
			if (writer != null) writer.writeHeader(reader.getHeader());
			if (intervals.isEmpty()) {
				for (final VariantContext variant : reader) {
					if (isCancelled()) break;
					process(writer, variant);
				}
			} else {
				for (Interval interval : intervals) {
					System.out.println("Querying " + interval);
					final VariantContextIterable iterable = new VariantContextIterable(reader.query(interval));
					for (VariantContext variant : iterable) {
						System.out.println(variant);
						if (isCancelled()) break;
						process(writer, variant);
					}
					System.out.println("Done");
				}
			}
		} catch (Throwable any) {
			any.printStackTrace();
			MainView.error(any);
		}
		return variants;
	}

	private void process(VariantContextWriter writer, VariantContext variant) {
		if (filters.stream().allMatch(filter -> filter.filter(variant))) {
			filtered.set(filtered.get() + 1);
			if (filtered.get() <= maxResults) {
				variants.add(variant);
			}
			if (writer != null) writer.add(variant);
		}
		lines.set(lines.get() + 1);
		if (lines.get() % 1000 == 0) {
			final String message = output == null
				? "%s:%,d".formatted(variant.getContig(), variant.getStart())
				: "Saving to %s (%s:%,d)".formatted(output.getName(), variant.getContig(), variant.getStart());
			updateMessage(message);
			if (total != null) {
				updateProgress(lines.get(), total);
			}
		}
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

	class VariantContextIterable implements Iterable<VariantContext> {

		private final Iterator<VariantContext> iterator;

		VariantContextIterable(Iterator<VariantContext> iterator) {this.iterator = iterator;}

		@Override
		public Iterator<VariantContext> iterator() {
			return iterator;
		}
	}
}
