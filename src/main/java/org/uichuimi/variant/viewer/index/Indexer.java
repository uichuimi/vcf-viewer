package org.uichuimi.variant.viewer.index;

import htsjdk.tribble.TribbleException;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import javafx.concurrent.Task;
import org.uichuimi.variant.viewer.utils.Chromosome;
import org.uichuimi.variant.viewer.utils.GenomeProgress;
import org.uichuimi.variant.viewer.utils.ResourceConsumer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

public class Indexer extends Task<VcfIndex> {

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

}
