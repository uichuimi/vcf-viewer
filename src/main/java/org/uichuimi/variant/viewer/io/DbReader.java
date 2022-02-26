package org.uichuimi.variant.viewer.io;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.*;
import org.uichuimi.variant.viewer.database.DataContainer;
import org.uichuimi.variant.viewer.database.FieldType;
import org.uichuimi.variant.viewer.database.Model;
import org.uichuimi.variant.viewer.utils.CollectionUtils;
import org.uichuimi.variant.viewer.utils.VcfUtils;

import java.io.File;
import java.util.*;

public class DbReader {

	private static final String MODEL_REFERENCES = "references";
	private static final String MODEL_PROPERTIES = "properties";
	private static final String MODEL_ALTERNATIVES = "alternatives";
	private static final String MODEL_SAMPLE_ALTERNATIVE = "sample_alternative";
	private static final String MODEL_SAMPLE_REFERENCE = "sample_reference";

	final Map<VCFHeaderLineType, FieldType> TYPE_MAP = Map.of(
			VCFHeaderLineType.Flag, FieldType.Boolean,
			VCFHeaderLineType.Integer, FieldType.Integer,
			VCFHeaderLineType.Float, FieldType.Float,
			VCFHeaderLineType.String, FieldType.String,
			VCFHeaderLineType.Character, FieldType.String
	);

	public DbReader(File file) {
		final VCFFileReader reader = new VCFFileReader(file, false);
		final DataContainer container = createDataContainer();
		final DatabaseExtractor databaseExtractor = createStructure(container, reader.getFileHeader());
		fillChromosomes(container, reader.getFileHeader());
		fillSamples(container, reader.getFileHeader());
		fillVariants(container, reader, databaseExtractor);
		for (Model model : container.getModels()) {
			System.out.println();
			System.out.println(" - " + model.getName());
			model.display();
		}
	}

	private DataContainer createDataContainer() {
		final DataContainer container = new DataContainer();
		// - CHROM, POS, REF, QUAL
		// - INFO with Number=0 or Number=1
		// - first value of INFO with Number=R
		final Model references = container.addModel(MODEL_REFERENCES);
		references.addField("id", FieldType.Integer);
		references.addField("chromosome_id", FieldType.Integer);
		references.addField("position", FieldType.Integer);
		references.addField("sequence", FieldType.String);
		references.addField("quality", FieldType.Float);

		// - ID, FILTER
		// - INFO with Number>1 or Number=.
		final Model properties = container.addModel(MODEL_PROPERTIES);
		properties.addField("reference_id", FieldType.Integer);
		properties.addField("rsid", FieldType.String);
		properties.addField("filter", FieldType.String);

		// INFO with Number=A or Number=R (except first value)
		final Model alternatives = container.addModel(MODEL_ALTERNATIVES);
		alternatives.addField("id", FieldType.Integer);
		alternatives.addField("reference_id", FieldType.Integer);
		alternatives.addField("sequence", FieldType.String);

		// INFO with Number=G
		final Model genotypes = container.addModel("genotypes");
		genotypes.addField("reference_id", FieldType.Integer);
		genotypes.addField("index", FieldType.Integer);

		// - All data from ##contig
		final Model chromosomes = container.addModel("chromosomes");
		chromosomes.addField("id", FieldType.Integer);
		chromosomes.addField("length", FieldType.Integer);

		// - The list of genotype samples
		final Model samples = container.addModel("samples");
		samples.addField("id", FieldType.Integer);
		samples.addField("name", FieldType.String);

		// - FORMAT with Number=0 or Number=1
		// - first value of FORMAT with Number=R
		final Model sampleReference = container.addModel(MODEL_SAMPLE_REFERENCE);
		sampleReference.addField("reference_id", FieldType.Integer);
		sampleReference.addField("sample_id", FieldType.Integer);

		// FORMAT with Number>1 or Number=.
		final Model sampleReferenceProperties = container.addModel("sample_reference_properties");
		sampleReferenceProperties.addField("reference_id", FieldType.Integer);
		sampleReferenceProperties.addField("sample_id", FieldType.Integer);

		// FORMAT with Number=A and Number=R (except first value)
		final Model sampleAlternative = container.addModel(MODEL_SAMPLE_ALTERNATIVE);
		sampleAlternative.addField("alternative_id", FieldType.Integer);
		sampleAlternative.addField("sample_id", FieldType.Integer);

		// FORMAT with Number=G
		final Model sampleGenotype = container.addModel("sample_genotype");
		sampleGenotype.addField("reference_id", FieldType.Integer);
		sampleGenotype.addField("sample_id", FieldType.Integer);
		sampleGenotype.addField("index", FieldType.Integer);

		return container;
	}

	private DatabaseExtractor createStructure(DataContainer container, VCFHeader header) {
		DatabaseExtractor databaseExtractor = new DatabaseExtractor();

		databaseExtractor.setBaseExtractor(MODEL_PROPERTIES, "rsid", variant -> {
			final String id = variant.getID();
			return id == null || id.equals(".") ? null : id.split(",");
		});
		databaseExtractor.setBaseExtractor(MODEL_PROPERTIES, "filter", variant -> new ArrayList<>(variant.getFilters()));
		databaseExtractor.setBaseExtractor(MODEL_ALTERNATIVES, "sequence", variant -> CollectionUtils.map(Allele::getBaseString, variant.getAlternateAlleles()));
		extractFields(header.getInfoHeaderLines(), container, databaseExtractor);
		extractSampleFields(header.getFormatHeaderLines(), container, databaseExtractor);

		return databaseExtractor;
	}

	private void extractFields(
			Collection<? extends VCFCompoundHeaderLine> headerLines,
			DataContainer container,
			DatabaseExtractor databaseExtractor) {
		databaseExtractor.setBaseExtractor(MODEL_REFERENCES, "quality", VariantContext::getPhredScaledQual);
		databaseExtractor.setBaseExtractor(MODEL_REFERENCES, "position", VariantContext::getStart);
		databaseExtractor.setBaseExtractor(MODEL_REFERENCES, "sequence", variant -> variant.getReference().getBaseString());
		for (VCFCompoundHeaderLine line : headerLines) {
			final BaseValueExtractor valueExtractor = variant -> variant.getAttribute(line.getID());
			switch (line.getCountType()) {
				case INTEGER -> {
					if (line.getCount() == 1) {
						container.getModel(MODEL_REFERENCES).addField(line.getID(), TYPE_MAP.get(line.getType()));
						databaseExtractor.setBaseExtractor(MODEL_REFERENCES, line.getID(), valueExtractor);
					} else if (line.getCount() == 0) {
						assert line.getType() == VCFHeaderLineType.Flag;
						container.getModel(MODEL_REFERENCES).addField(line.getID(), FieldType.Boolean);
						databaseExtractor.setBaseExtractor(MODEL_REFERENCES, line.getID(), valueExtractor);
					} else {
						// todo individual tables for each column
						container.getModel(MODEL_PROPERTIES).addField(line.getID(), TYPE_MAP.get(line.getType()));
						databaseExtractor.setBaseExtractor(MODEL_PROPERTIES, line.getID(), valueExtractor);
					}
				}
				case UNBOUNDED -> {
					container.getModel(MODEL_PROPERTIES).addField(line.getID(), TYPE_MAP.get(line.getType()));
					databaseExtractor.setBaseExtractor(MODEL_PROPERTIES, line.getID(), valueExtractor);
				}
				case A -> {
					container.getModel(MODEL_ALTERNATIVES).addField(line.getID(), TYPE_MAP.get(line.getType()));
					databaseExtractor.setBaseExtractor(MODEL_ALTERNATIVES, line.getID(), valueExtractor);
				}
				case R -> {
					container.getModel(MODEL_REFERENCES).addField(line.getID(), TYPE_MAP.get(line.getType()));
					container.getModel(MODEL_ALTERNATIVES).addField(line.getID(), TYPE_MAP.get(line.getType()));
					databaseExtractor.setBaseExtractor(MODEL_REFERENCES, line.getID(), variant -> CollectionUtils.first(variant.getAttribute(line.getID())));
					databaseExtractor.setBaseExtractor(MODEL_ALTERNATIVES, line.getID(), variant -> CollectionUtils.skip(1, variant.getAttribute(line.getID())));
				}
				case G -> {
					container.getModel("genotypes").addField(line.getID(), TYPE_MAP.get(line.getType()));
					databaseExtractor.setBaseExtractor("genotypes", line.getID(), valueExtractor);
				}
			}
		}
	}

	private void extractSampleFields(Collection<VCFFormatHeaderLine> headerLines, DataContainer container, DatabaseExtractor databaseExtractor) {
		for (VCFFormatHeaderLine line : headerLines) {
			final var standardFormatFields = Set.of("GT", "GQ", "AD", "PL", "DP", "FT");
			if (line.getID().equals("GT")) {
				// Number 1 (:smirk:)
				databaseExtractor.setSampleExtractor(MODEL_SAMPLE_REFERENCE, "GT", Genotype::getType);
				container.getModel(MODEL_SAMPLE_REFERENCE).addField("GT", FieldType.String);  // temp here
				continue;
			}
			if (line.getID().equals("GQ")) {
				databaseExtractor.setSampleExtractor(MODEL_SAMPLE_REFERENCE, "GQ", genotype -> genotype.getGQ() < 0 ? null : genotype.getGQ());
				container.getModel(MODEL_SAMPLE_REFERENCE).addField("GQ", FieldType.Integer);  // Number=1
				continue;
			}
			if (line.getID().equals("DP")) {
				databaseExtractor.setSampleExtractor(MODEL_SAMPLE_REFERENCE, "DP", genotype -> genotype.getDP() < 0 ? null : genotype.getDP());
				container.getModel(MODEL_SAMPLE_REFERENCE).addField("DP", FieldType.Integer);  // Number=1
				continue;
			}
			if (line.getID().equals("AD")) {  // Number=R
				databaseExtractor.setSampleExtractor(MODEL_SAMPLE_REFERENCE, "AD", genotype -> CollectionUtils.first(genotype.getAD()));
				databaseExtractor.setSampleExtractor(MODEL_SAMPLE_ALTERNATIVE, "AD", genotype -> CollectionUtils.skip(1, genotype.getAD()));
				container.getModel(MODEL_SAMPLE_REFERENCE).addField("AD", FieldType.Integer);
				container.getModel(MODEL_SAMPLE_ALTERNATIVE).addField("AD", FieldType.Integer);
				continue;
			}
			if (line.getID().equals("PL")) { // Number=G
				databaseExtractor.setSampleExtractor("sample_genotype", "PL", Genotype::getPL);
				container.getModel("sample_genotype").addField("PL", FieldType.Integer);
				continue;
			}
			if (line.getID().equals("FT")) { // Number=1
				databaseExtractor.setSampleExtractor(MODEL_SAMPLE_REFERENCE, "FT", Genotype::getFilters);
				container.getModel(MODEL_SAMPLE_REFERENCE).addField("FT", FieldType.String);
				continue;
			}
			final boolean singleValued = line.getCountType() == VCFHeaderLineCount.INTEGER && line.getCount() <= 1;
			final SampleValueExtractor valueExtractor = genotype -> VcfUtils. parse(genotype.getExtendedAttribute(line.getID()), TYPE_MAP.get(line.getType()), !singleValued);
			switch (line.getCountType()) {
				case INTEGER -> {
					if (line.getCount() == 1) {
						container.getModel(MODEL_SAMPLE_REFERENCE).addField(line.getID(), TYPE_MAP.get(line.getType()));
						databaseExtractor.setSampleExtractor(MODEL_SAMPLE_REFERENCE, line.getID(), valueExtractor);
					} else if (line.getCount() == 0) {
						assert line.getType() == VCFHeaderLineType.Flag;
						container.getModel(MODEL_SAMPLE_REFERENCE).addField(line.getID(), FieldType.Boolean);
						databaseExtractor.setSampleExtractor(MODEL_SAMPLE_REFERENCE, line.getID(), valueExtractor);
					} else {
						// todo individual tables for each column
						container.getModel("sample_reference_properties").addField(line.getID(), TYPE_MAP.get(line.getType()));
						databaseExtractor.setSampleExtractor("sample_reference_properties", line.getID(), valueExtractor);
					}
				}
				case UNBOUNDED -> {
					container.getModel("sample_reference_properties").addField(line.getID(), TYPE_MAP.get(line.getType()));
					databaseExtractor.setSampleExtractor("sample_reference_properties", line.getID(), valueExtractor);
				}
				case A -> {
					container.getModel(MODEL_SAMPLE_ALTERNATIVE).addField(line.getID(), TYPE_MAP.get(line.getType()));
					databaseExtractor.setSampleExtractor(MODEL_SAMPLE_ALTERNATIVE, line.getID(), valueExtractor);
				}
				case R -> {
					container.getModel(MODEL_SAMPLE_REFERENCE).addField(line.getID(), TYPE_MAP.get(line.getType()));
					container.getModel(MODEL_SAMPLE_ALTERNATIVE).addField(line.getID(), TYPE_MAP.get(line.getType()));
					databaseExtractor.setSampleExtractor(MODEL_SAMPLE_REFERENCE, line.getID(),
							genotype -> CollectionUtils.first(genotype.getAnyAttribute(line.getID())));
					databaseExtractor.setSampleExtractor(MODEL_SAMPLE_ALTERNATIVE, line.getID(),
							genotype -> CollectionUtils.skip(1, genotype.getAnyAttribute(line.getID())));
				}
				case G -> {
					container.getModel("genotypes").addField(line.getID(), TYPE_MAP.get(line.getType()));
					databaseExtractor.setSampleExtractor("genotypes", line.getID(), valueExtractor);
				}
			}
		}
	}

	private void fillSamples(DataContainer container, VCFHeader header) {
		final Model samples = container.getModel("samples");
		for (String name : header.getGenotypeSamples()) {
			samples.addRecord(new Object[]{container.nextId("sample_sequence"), name});
		}
	}

	private void fillChromosomes(DataContainer container, VCFHeader header) {
		final Model chromosomes = container.getModel("chromosomes");
		if (!header.getContigLines().isEmpty()) {
			// Create table structure
			for (String key : header.getContigLines().get(0).getGenericFields().keySet()) {
				if (!key.equals("length")) {
					chromosomes.addField(key, FieldType.String);
				}
			}
			// Fill content
			for (VCFContigHeaderLine line : header.getContigLines()) {
				final Map<String, Object> values = new HashMap<>(line.getGenericFields());
				values.put("length", Integer.parseInt((String) values.get("length")));
				values.put("id", container.nextId("chromosome_sequence"));
				chromosomes.create(values);
			}
		}
	}

	private void fillVariants(DataContainer container, VCFFileReader reader, DatabaseExtractor databaseExtractor) {
		for (VariantContext variant : reader) {
			int referenceId = populateReference(container, variant, databaseExtractor);
			populateProperties(referenceId, container, variant, databaseExtractor);
			populateGenotypes(referenceId, container, variant, databaseExtractor);
			populateAlternatives(referenceId, container, variant, databaseExtractor);


			//
			//		// FORMAT with Number>1 or Number=.
			//		final Model sampleReferenceProperties = container.addModel("sample_reference_properties");
			//		sampleReferenceProperties.addField("reference_id", FieldType.Integer);
			//		sampleReferenceProperties.addField("sample_id", FieldType.Integer);
			//		// HQ (Number=2)
			//		sampleReferenceProperties.addField("haplotype_quality", FieldType.Integer);
			//
			//		// FORMAT with Number=A and Number=R (except first value)
			//		final Model sampleAlternative = container.addModel("sample_alternative");
			//		sampleAlternative.addField("alternative_id", FieldType.Integer);
			//		sampleAlternative.addField("sample_id", FieldType.Integer);
			//		sampleAlternative.addField("allele_depth", FieldType.Float);  // Number=R
			//
			//		// FORMAT with Number=G
			//		final Model sampleGenotype = container.addModel("sample_genotype");
			//		sampleGenotype.addField("reference_id", FieldType.Integer);
			//		sampleGenotype.addField("sample_id", FieldType.Integer);
			//		sampleGenotype.addField("index", FieldType.Integer);
			//		sampleGenotype.addField("phred_likelihood", FieldType.Integer);
			populateSampleReference(referenceId, container, variant, databaseExtractor);
			populateSampleReferenceProperties(referenceId, container, variant, databaseExtractor);
		}
	}

	private int populateReference(DataContainer container, VariantContext variant, DatabaseExtractor databaseExtractor) {
		final Model references = container.getModel(MODEL_REFERENCES);
		final Model chromosomes = container.getModel("chromosomes");

		final var values = new HashMap<String, Object>();
		values.put("id", container.nextId("references_sequence"));
		var chromosomeId = chromosomes.filter("ID", id -> id.equals(variant.getContig())).getOne("id");
		values.put("chromosome_id", chromosomeId);
		databaseExtractor.getBaseExtractorMap(MODEL_REFERENCES).forEach((key, extractor) -> {
			final Object value = extractor.extract(variant);
			if (value != null) {
				values.put(key, value);
			}
		});
		references.create(values);
		return (int) values.get("id");
	}

	private void populateProperties(int referenceId, DataContainer container, VariantContext variant, DatabaseExtractor databaseExtractor) {
		populateArrayInfo(referenceId, container, variant, databaseExtractor, MODEL_PROPERTIES);
	}

	private void populateGenotypes(int referenceId, DataContainer container, VariantContext variant, DatabaseExtractor databaseExtractor) {
		populateArrayInfo(referenceId, container, variant, databaseExtractor, "genotypes");
	}

	private void populateAlternatives(int referenceId, DataContainer container, VariantContext variant, DatabaseExtractor databaseExtractor) {
		populateArrayInfo(referenceId, container, variant, databaseExtractor, MODEL_ALTERNATIVES);
	}

	private void populateSampleReference(int referenceId, DataContainer container, VariantContext variant, DatabaseExtractor databaseExtractor) {
		for (Genotype genotype : variant.getGenotypes()) {
			// todo do it only once
			final Object sampleId = container.getModel("samples").filter("name", n -> n.equals(genotype.getSampleName())).getOne("id");
			final Model references = container.getModel(MODEL_SAMPLE_REFERENCE);
// // - FORMAT with Number=0 or Number=1
			//		// - first value of FORMAT with Number=R
			//		// - GQ, AD[0]
			//		// - temp: GT (Number=1), GQ, AD, DP
			//		final Model sampleReference = container.addModel("sample_reference");
			//		sampleReference.addField("reference_id", FieldType.Integer);
			//		sampleReference.addField("sample_id", FieldType.Integer);
			//		sampleReference.addField("genotype", FieldType.String);  // temp here
			//		sampleReference.addField("genotype_quality", FieldType.Float);  // Number=1
			//		sampleReference.addField("depth", FieldType.Float);  // Number=1
			//		sampleReference.addField("allele_depth", FieldType.Float);  // Number=R
			final var values = new HashMap<String, Object>();
			values.put("reference_id", referenceId);
			values.put("sample_id", sampleId);
			databaseExtractor.getSampleExtractorMap(MODEL_SAMPLE_REFERENCE).forEach((key, extractor) -> {
				final Object value = extractor.extract(genotype);
				if (value != null) {
					values.put(key, value);
				}
			});
			references.create(values);
		}

	}

	private void populateSampleReferenceProperties(int referenceId, DataContainer container, VariantContext variant, DatabaseExtractor databaseExtractor) {
		populateArraySampleInfo(referenceId, container, variant, databaseExtractor, "sample_reference_properties");
	}

	private void populateArrayInfo(int referenceId, DataContainer container, VariantContext variant, DatabaseExtractor databaseExtractor, String table) {
		final List<Map<String, Object>> records = new ArrayList<>();
		databaseExtractor.getBaseExtractorMap(table).forEach((key, extractor) -> {
			Object object = extractor.extract(variant);
			final List<?> values = CollectionUtils.toList(object);
			// .,.,.,.
			if (values.stream().allMatch(Objects::isNull)) {
				return;
			}
			for (int i = 0; i < values.size(); i++) {
				if (records.size() <= i) {
					records.add(new HashMap<>());
				}
				records.get(i).put(key, values.get(i));
			}
		});
		for (int index = 0; index < records.size(); index++) {
			Map<String, Object> record = records.get(index);
			record.put("reference_id", referenceId);
			if (container.getModel(table).hasField("id")) {
				record.put("id", container.nextId(table + "_sequence"));
			}
			if (container.getModel(table).hasField("index")) {
				record.put("index", index);
			}
			container.getModel(table).create(record);
		}
	}

	private void populateArraySampleInfo(int referenceId, DataContainer container, VariantContext variant, DatabaseExtractor databaseExtractor, String table) {
		for (int j = 0; j < variant.getGenotypes().size(); j++) {
			final List<Map<String, Object>> records = new ArrayList<>();
			Genotype genotype = variant.getGenotypes().get(j);
			var sample_id = container.getModel("samples").filter("name", n -> n.equals(genotype.getSampleName())).getOne("id");
			databaseExtractor.getSampleExtractorMap(table).forEach((key, extractor) -> {
				Object object = extractor.extract(genotype);
				final List<?> values = CollectionUtils.toList(object, true);
				// .,.,.,.
				if (values.stream().allMatch(Objects::isNull)) {
					return;
				}
				for (int i = 0; i < values.size(); i++) {
					if (records.size() <= i) {
						records.add(new HashMap<>());
					}
					records.get(i).put(key, values.get(i));
				}
			});
			for (int index = 0; index < records.size(); index++) {
				Map<String, Object> record = records.get(index);
				record.put("reference_id", referenceId);
				record.put("sample_id", sample_id);
				if (container.getModel(table).hasField("id")) {
					record.put("id", container.nextId(table + "_sequence"));
				}
				if (container.getModel(table).hasField("index")) {
					record.put("index", index);
				}
				container.getModel(table).create(record);
			}
		}
	}
}
