package org.uichuimi.variant.viewer.index;

import htsjdk.variant.variantcontext.GenotypeType;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.uichuimi.variant.viewer.utils.BitUtils;
import org.uichuimi.variant.viewer.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GtBitsetArchiveBuilder implements AutoCloseable {

	private static final List<GenotypeType> typeList = Constants.validGenotypeTypes();
	private final File file;
	private final List<String> people;
	private final int numberOfWords;
	private final ZipOutputStream zipFile;

	private String contig;
	private Map<Integer, long[]> gts;

	public GtBitsetArchiveBuilder(final File file, VCFHeader header) throws IOException {
		this.file = file;
		this.zipFile = new ZipOutputStream(new FileOutputStream(file));
		people = header.getGenotypeSamples();
		numberOfWords = (int) Math.ceil(1.0 * people.size() * typeList.size() / 64);
	}

	public void addSite(VariantContext variant) throws IOException {
		final long[] bitSet = createGenotypeBitSet(variant);
		if (!variant.getContig().equals(contig)) {
			storeContig();
			contig = variant.getContig();
			gts = new LinkedHashMap<>();
		}
		gts.put(variant.getStart(), bitSet);
	}

	private void storeContig() throws IOException {
		if (contig == null) return;
		final GtBitset gtBitset = new GtBitset(contig, 0, Integer.MAX_VALUE, gts);
		final ZipEntry entry = new ZipEntry(contig);
		zipFile.putNextEntry(entry);
		new ObjectOutputStream(zipFile).writeObject(gtBitset);
	}

	public GtBitsetArchive getArchive() {
		return new GtBitsetArchive(file);
	}

	private long[] createGenotypeBitSet(final VariantContext variant) {
		final long[] bitset = new long[numberOfWords];
		for (int i = 0; i < people.size(); i++) {
			final GenotypeType genotypeType = variant.getGenotype(i).getType();
			int type = typeList.indexOf(genotypeType);
			// We assume UNAVAILABLE and MIXED to be NO_CALL
			if (type < 0) type = 0;
			BitUtils.set(bitset, typeList.size() * i + type);
		}
		return bitset;
	}

	@Override
	public void close() throws Exception {
		storeContig();
		zipFile.close();
	}
}
