package org.uichuimi.variant.viewer.index;

import org.uichuimi.variant.viewer.filter.Field;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class VcfIndex implements Serializable {

	@Serial
	private static final long serialVersionUID = 3192814140848325947L;

	private final List<Field> fields;
	private final long lineCount;
	private final GtBitsetArchive archive;
	private final List<String> samples;

	public VcfIndex(final List<Field> fields, final long lineCount, GtBitsetArchive archive, List<String> samples) {
		this.fields = fields;
		this.lineCount = lineCount;
		this.archive = archive;
		this.samples = samples;
	}

	public List<Field> getFields() {
		return fields;
	}

	public long getLineCount() {
		return lineCount;
	}

	public long[] getBitSet(String contig, int position) {
		return archive.getBitSet(contig, position);
	}

	public List<String> getSamples() {
		return samples == null ? List.of() : samples;
	}
}
