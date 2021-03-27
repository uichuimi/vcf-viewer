package org.uichuimi.variant;

import org.uichuimi.variant.viewer.components.filter.Field;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VcfIndex implements Serializable {

	@Serial
	private static final long serialVersionUID = 3192814140848325947L;

	private final List<Field> fields;
	private final long lineCount;

	public VcfIndex(final Map<String, Set<String>> options, final List<Field> fields, final long lineCount) {
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
