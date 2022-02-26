package org.uichuimi.variant.viewer.io;

import java.util.HashMap;
import java.util.Map;

public class DatabaseExtractor {

	private final Map<String, Map<String, BaseValueExtractor>> baseExtractors = new HashMap<>();
	private final Map<String, Map<String, SampleValueExtractor>> sampleExtractors = new HashMap<>();

	public void setBaseExtractor(String model, String field, BaseValueExtractor extractor) {
		baseExtractors.computeIfAbsent(model, m -> new HashMap<>()).put(field, extractor);
	}

	public void setSampleExtractor(String model, String field, SampleValueExtractor extractor) {
		sampleExtractors.computeIfAbsent(model, m -> new HashMap<>()).put(field, extractor);
	}

	public BaseValueExtractor getBaseExtractor(String model, String field) {
		return getBaseExtractorMap(model).get(field);
	}

	public Map<String, BaseValueExtractor> getBaseExtractorMap(String model) {
		return baseExtractors.getOrDefault(model, Map.of());
	}

	public Map<String, SampleValueExtractor> getSampleExtractorMap(String model) {
		return sampleExtractors.getOrDefault(model, Map.of());
	}
}
