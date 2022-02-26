package org.uichuimi.variant.viewer.database;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DataContainer {

	private final Map<String, Model> models = new HashMap<>();
	private final Map<String, AtomicInteger> sequences = new HashMap<>();

	public Model getModel(String name) {
		return models.get(name);
	}

	public Model addModel(String name) {
		if (models.containsKey(name)) {
			throw new IllegalArgumentException(name + " table already exists");
		}
		final Model model = new Model(name);
		models.put(name, model);
		return model;
	}
	public void addModel(Model model) {
		models.put(model.getName(), model);
	}

	public int nextId(String sequence) {
		return sequences.computeIfAbsent(sequence, s -> new AtomicInteger()).incrementAndGet();
	}

	public Collection<Model> getModels() {
		return models.values();
	}

}
