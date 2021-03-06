package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;

/**
 * Shows a table indicating for each population and source, its allele count (AC), allele number (AN) and allele
 * frequency (AF).
 */
public class FrequenciesTable {

	private static final List<String> SEPARATORS = List.of("_", "-");
	private static final String NONE = "-";
	private static final String AF = "af";
	private static final String AC = "ac";
	private static final String AN = "an";
	private static final List<String> FREQUENCY_IDS = List.of(AC, AN, AF);
	private static final Collection<String> STANDARD_POPULATIONS = Set.of("afr", "ami", "amr", "asj", "eas", "fin", "mid", "nfe", "oth");

	@FXML
	private TableColumn<Frequency, String> source;
	@FXML
	private TableColumn<Frequency, String> population;
	@FXML
	private TableColumn<Frequency, Integer> number;
	@FXML
	private TableColumn<Frequency, Integer> count;
	@FXML
	private TableColumn<Frequency, Double> frequency;
	@FXML
	private TableView<Frequency> frequencies;

	private Collection<FrequencyFactory> factories;

	public void setHeader(VCFHeader header) {
		createFactories(header);
	}

	public void select(VariantContext variant) {
		frequencies.getItems().clear();
		if (variant == null) {
			return;
		}
		for (FrequencyFactory factory : factories) {
			final Frequency frequency = factory.build(variant);
			if (frequency != null) {
				frequencies.getItems().add(frequency);
			}
		}
	}

	public List<String> frequencyFields() {
		final List<String> fields = new ArrayList<>();
		for (FrequencyFactory factory : factories) {
			fields.add(factory.af);
			fields.add(factory.an);
			fields.add(factory.ac);
		}
		return fields;
	}

	private void createFactories(VCFHeader header) {
		factories = new LinkedHashSet<>();
		// Get a list of field ids containing AC, AF or AN
		final List<String> candidates = getCandidates(header);
		// Try to split every candidate by _ or -, subtract the AC, AF or AN string, and guess which of the others
		// refer to a population or a source
		final Collection<String> populations = determinePopulations(candidates);
		// Again, split each candidate by _ or - and create a FrequencyFactory for each source and population
		// finding the trio [src,pop,ac], [src,pop,an], [src,pop,af]
		for (String separator : SEPARATORS) {
			factories.addAll(createFactories(separator, candidates, populations));
		}
	}

	/**
	 * Finds which strings represent a population and which a source. Since naming has no convention, it is not possible
	 * to know which item in a field id is source neither population.
	 */
	private Collection<String> determinePopulations(List<String> candidates) {
		// We need to find 3 elements IDs, one of the elements must be AC, AF or AN.
		// If one of the others is a standard population, the other is a source.
		// All strings not being a source are considered populations
		final Collection<String> sources = new TreeSet<>();
		final Collection<String> populations = new TreeSet<>();
		final Collection<String> undefined = new TreeSet<>();
		for (String separator : SEPARATORS) {
			for (String candidate : candidates) {
				final String[] split = candidate.split(separator);
				if (split.length == 3) {
					// Which positions are using the AC/AF/AN and the population?
					final Set<Integer> index = new TreeSet<>();
					for (int i = 0; i < split.length; i++) {
						final String val = split[i].toLowerCase(Locale.ROOT);
						if (FREQUENCY_IDS.contains(val)) {
							index.add(i);
						} else if (STANDARD_POPULATIONS.contains(val)) {
							index.add(i);
							populations.add(split[i]);
						}
					}
					// Did we find positions for these 2 elements?
					if (index.size() == 2) {
						// Let's use the unused position for the source
						for (int i = 0; i < 3; i++) {
							if (!index.contains(i)) {
								sources.add(split[i]);
							}
						}
					} else {
						// This is a 3-value split with no known population
						Collections.addAll(undefined, split);
					}
				} else if (split.length == 2) {
					if (FREQUENCY_IDS.contains(split[0].toLowerCase(Locale.ROOT))) {
						undefined.add(split[1]);
					} else if (FREQUENCY_IDS.contains(split[1].toLowerCase(Locale.ROOT))) {
						undefined.add(split[0]);
					}
				}
			}
		}
		undefined.removeAll(sources);
		for (String val : new ArrayList<>(undefined)) {
			final String v = val.toLowerCase(Locale.ROOT);
			if (FREQUENCY_IDS.contains(v)) {
				undefined.remove(val);
			}
		}
		populations.addAll(undefined);
		return populations;
	}

	private List<String> getCandidates(VCFHeader header) {
		final List<String> candidates = new ArrayList<>();
		for (VCFInfoHeaderLine line : header.getInfoHeaderLines()) {
			final String id = line.getID().toLowerCase(Locale.ROOT);
			if (id.contains(AF) || id.contains(AN) || id.contains(AC)) {
				candidates.add(line.getID());
			}
		}
		return candidates;
	}

	private Collection<FrequencyFactory> createFactories(String separator, List<String> candidates, Collection<String> populations) {
		// We need to identify any combination of the following pattern:
		// - the string AF, AN or AC (upper or lower)
		// - a separator (usually _ or -)
		// - (optional) a string representing a population
		// - (optional) a string representing a source
		// For example:
		// GG_AF_afr
		// GG_afr_af
		// AF_GG_afr
		// AF_afr_GG
		// afr_AF_GG
		// afr_GG_AF
		// If only two values in the string, only two combinations available
		// AF_afr
		// afr_AF
		// If no separator exists, only AF,AC and AN strings are considered

		candidates = new ArrayList<>(candidates);
		final List<FrequencyFactory> factories = new ArrayList<>();
		while (!candidates.isEmpty()) {
			final String candidate = candidates.get(0);
			final String[] split = candidate.split(separator);
			String population = null;
			String source = null;
			String[] afFields = null;
			String[] anFields = null;
			String[] acFields = null;

			if (split.length == 1) {
				// Only 1 value, then it must be AC|AN|AF
				acFields = new String[]{AC};
				afFields = new String[]{AF};
				anFields = new String[]{AN};
			} else if (split.length == 2) {
				// 2 values, one is AC|AN|AF, the other is a population or source
				if (FREQUENCY_IDS.contains(split[0].toLowerCase(Locale.ROOT))) {
					if (populations.contains(split[1])) {
						population = split[1];
					} else {
						source = split[1];
					}
					afFields = new String[]{AF, split[1]};
					acFields = new String[]{AC, split[1]};
					anFields = new String[]{AN, split[1]};
				} else if (FREQUENCY_IDS.contains(split[1].toLowerCase(Locale.ROOT))) {
					if (populations.contains(split[0])) {
						population = split[0];
					} else {
						source = split[0];
					}
					afFields = new String[]{split[0], AF};
					acFields = new String[]{split[0], AC};
					anFields = new String[]{split[0], AN};
				}
			} else if (split.length == 3) {
				// 3 values, one is AC|AN|AF, the other is a population or source
				if (FREQUENCY_IDS.contains(split[0].toLowerCase(Locale.ROOT))) {
					if (populations.contains(split[1])) {
						population = split[1];
						source = split[2];
					} else {
						source = split[1];
						population = split[2];
					}
					afFields = new String[]{AF, split[1], split[2]};
					acFields = new String[]{AC, split[1], split[2]};
					anFields = new String[]{AN, split[1], split[2]};
				} else if (FREQUENCY_IDS.contains(split[1].toLowerCase(Locale.ROOT))) {
					if (populations.contains(split[0])) {
						population = split[0];
						source = split[2];
					} else {
						source = split[0];
						population = split[2];
					}
					afFields = new String[]{split[0], AF, split[2]};
					acFields = new String[]{split[0], AC, split[2]};
					anFields = new String[]{split[0], AN, split[2]};
				} else if (FREQUENCY_IDS.contains(split[2].toLowerCase(Locale.ROOT))) {
					if (populations.contains(split[0])) {
						population = split[0];
						source = split[1];
					} else {
						source = split[0];
						population = split[1];
					}
					afFields = new String[]{split[0], split[1], AF};
					acFields = new String[]{split[0], split[1], AC};
					anFields = new String[]{split[0], split[1], AN};
				}
			}
			// Et voil??, we have all the ingredients: src, pop and ac|an|af strings
			if (ObjectUtils.allNotNull(acFields, anFields, afFields)) {
				final String ac = find(candidates, buildFrequencyString(separator, acFields));
				final String an = find(candidates, buildFrequencyString(separator, anFields));
				final String af = find(candidates, buildFrequencyString(separator, afFields));
				if (ObjectUtils.anyNotNull(ac, af, an)) {
					factories.add(new FrequencyFactory(source, population, ac, an, af));
					candidates.remove(ac);
					candidates.remove(an);
					candidates.remove(af);
				}
			}
			candidates.remove(candidate);
		}
		return factories;
	}

	private String find(List<String> candidates, String lowerCaseId) {
		for (String candidate : candidates) {
			if (candidate.equalsIgnoreCase(lowerCaseId)) {
				return candidate;
			}
		}
		return null;
	}

	private String buildFrequencyString(String separator, String... fields) {
		return String.join(separator, fields);
	}

	@FXML
	private void initialize() {
		population.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().population));
		source.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().source));
		frequency.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().af));
		number.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().an));
		count.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().ac));
	}


	/**
	 * {@link Frequency}s are meant to be used as rows in a frequency table. They are immutable.
	 */
	private static class Frequency {
		final String source;
		final String population;
		final Integer ac;
		final Integer an;
		final Double af;

		private Frequency(String source, String population, Integer ac, Integer an, Double af) {
			this.source = source;
			this.population = population;
			this.ac = ac;
			this.an = an;
			this.af = af;
		}
	}

	/**
	 * Factories are created using a VCF header and can be used on any variant of the file. Each factory groups 3
	 * values: ac, an and af. When a variant is parsed to a factory, it returns an object of type {@link Frequency},
	 * which contains these 3 values. The factory must then contain the id of the info fields, as well as already parsed
	 * values for source and populating.
	 */
	private static class FrequencyFactory {

		private final String source;
		private final String population;
		private final String ac;
		private final String an;
		private final String af;

		private FrequencyFactory(String source, String population, String ac, String an, String af) {
			this.population = population == null ? NONE : population;
			this.source = source == null ? NONE : source;
			this.ac = ac;
			this.an = an;
			this.af = af;
		}

		public Frequency build(VariantContext variant) {
			final Integer count = getOneInt(ac, variant);
			final Integer number = getOneInt(an, variant);
			final Double freq = getOneDouble(af, variant);
			if (ObjectUtils.anyNotNull(count, number, freq)) {
				return new Frequency(source, population, count, number, freq);
			}
			return null;
		}

		private Integer getOneInt(String id, VariantContext variant) {
			final List<Integer> list = variant.getAttributeAsIntList(id, -1);
			if (list.isEmpty()) return null;
			return list.get(0);
		}

		private Double getOneDouble(String id, VariantContext variant) {
			final List<Double> list = variant.getAttributeAsDoubleList(id, -1);
			if (list.isEmpty()) return null;
			return list.get(0);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			FrequencyFactory that = (FrequencyFactory) o;
			return Objects.equals(source, that.source) &&
				Objects.equals(population, that.population) &&
				Objects.equals(ac, that.ac) &&
				Objects.equals(an, that.an) &&
				Objects.equals(af, that.af);
		}

		@Override
		public int hashCode() {
			return Objects.hash(source, population, ac, an, af);
		}
	}

}
