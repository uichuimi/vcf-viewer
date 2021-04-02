package org.uichuimi.variant.viewer.index;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GtBitsetArchive implements Serializable {

	@Serial
	private final static long serialVersionUID = 1655428541224075358L;

	private final File file;
	private transient GtBitset bitset;

	public GtBitsetArchive(final File file) {
		this.file = file;
	}

	public long[] getBitSet(String contig, int position) {
		if (bitset == null || !bitset.getChromosome().equals(contig) || bitset.getStart() > position || position > bitset.getEnd()) {
			loadBitSet(contig, position);
		}
		return bitset.getBitset(position);
	}

	private void loadBitSet(String contig, int position) {
		try (ZipFile zipFile = new ZipFile(file)) {
			final ZipEntry entry = zipFile.getEntry(contig);
			bitset = (GtBitset) new ObjectInputStream(zipFile.getInputStream(entry)).readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
