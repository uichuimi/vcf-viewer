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

	public VcfIndex(final List<Field> fields, final long lineCount) {
		this.fields = fields;
		this.lineCount = lineCount;
	}

	public List<Field> getFields() {
		return fields;
	}

	public long getLineCount() {
		return lineCount;
	}

}
