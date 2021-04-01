package org.uichuimi.variant;

import org.uichuimi.variant.viewer.components.filter.Field;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class VcfIndex implements Serializable {

	@Serial
	private static final long serialVersionUID = 3192814140848325947L;

	private final List<Field> fields;
	private final long lineCount;
	private final List<long[]> gts;

	public VcfIndex(final List<Field> fields, final long lineCount, final List<long[]> gts) {
		this.fields = fields;
		this.lineCount = lineCount;
		this.gts = gts;
	}

	public List<Field> getFields() {
		return fields;
	}

	public long getLineCount() {
		return lineCount;
	}

	public List<long[]> getGts() {
		return gts;
	}
}
