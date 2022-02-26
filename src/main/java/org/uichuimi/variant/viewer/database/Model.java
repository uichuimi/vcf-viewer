package org.uichuimi.variant.viewer.database;


import org.apache.commons.lang3.StringUtils;
import org.uichuimi.variant.viewer.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.uichuimi.variant.viewer.utils.CollectionUtils.map;

public class Model {

	private final String name;
	private final List<Field> fields;
	private final Map<String, Integer> fieldIndex;
	private final List<Object[]> records;

	public Model(String name) {
		this.name = name;
		fields = new ArrayList<>();
		fieldIndex = new HashMap<>();
		records = new ArrayList<>();
	}

	public Model(Model model, List<Object[]> records) {
		this.name = model.name;
		this.fields = model.fields;
		this.fieldIndex = model.fieldIndex;
		this.records = records;
	}

	public String getName() {
		return name;
	}

	public void addField(String name, FieldType type) {
		fieldIndex.put(name, fields.size());
		fields.add(new Field(name, type));
	}

	public void create(Map<String, Object> content) {
		final var values = new Object[fields.size()];
		for (Map.Entry<String, Object> entry : content.entrySet()) {
			if (!fieldIndex.containsKey(entry.getKey())) {
				throw new IllegalArgumentException(entry.getKey());
			}
			assert fieldIndex.containsKey(entry.getKey());
			values[fieldIndex.get(entry.getKey())] = entry.getValue();
		}
		records.add(values);
	}

	public void addRecord(Object[] values) {
		records.add(values);
	}

	@Override
	public String toString() {
		return "VcfTable " + name + " " + fields;
	}

	public List<Field> getFields() {
		return fields;
	}

	public void display() {
		final var columns = map(Field::getName, fields);
		final var data = new ArrayList<List<String>>();
		for (Object[] record : records) {
			data.add(map(obj -> obj == null ? "." : String.valueOf(obj), record));
		}
		final var widths = map(String::length, columns);
		for (List<String> row : data) {
			for (int colIndex = 0; colIndex < row.size(); colIndex++) {
				widths.set(colIndex, Math.max(widths.get(colIndex), row.get(colIndex).length()));
			}
		}
		final String separator = "  ";
		final List<String> fillers = fillers(widths);
		printRow(columns, widths, separator);
		printRow(fillers, widths, separator);
		for (List<String> row : data) {
			printRow(row, widths, separator);
		}
	}

	public Model filter(String column, Predicate<Object> filter) {
		final Integer index = fieldIndex.get(column);
		var recs = CollectionUtils.filter(objects -> filter.test(objects[index]), records);
		return new Model(this, recs);
	}

	public List<Object> get(String column) {
		final Integer index = fieldIndex.get(column);
		return map(objects -> objects[index], records);
	}

	public Object getOne(String column) {
		final Integer index = fieldIndex.get(column);
		return records.get(0)[index];
	}

	private List<String> fillers(List<Integer> widths) {
		return map(w -> StringUtils.repeat("-", w), widths);
	}

	private void printRow(List<String> data, List<Integer> widths, String separator) {
		final List<String> content = new ArrayList<>();
		for (int i = 0; i < data.size(); i++) {
			content.add(adjust(data.get(i), widths.get(i)));
		}
		System.out.println(String.join(separator, content));
	}

	private String adjust(String text, int width) {
		if (text.length() == width) {
			return text;
		}
		if (text.length() > width) {
			return text.substring(0, width);
		}
		return StringUtils.rightPad(text, width);
	}

	public boolean hasField(String fieldName) {
		return fieldIndex.containsKey(fieldName);
	}
}
