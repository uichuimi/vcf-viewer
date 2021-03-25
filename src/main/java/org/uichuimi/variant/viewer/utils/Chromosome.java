package org.uichuimi.variant.viewer.utils;

import htsjdk.variant.vcf.VCFContigHeaderLine;
import htsjdk.variant.vcf.VCFHeader;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Chromosome implements Comparable<Chromosome> {

	private static final String NA = "na";
	private static final List<Chromosome> chromosomes = new ArrayList<>(ChromosomeFactory.getChromosomeList());
	private static final Map<Chromosome.Namespace, Map<String, Chromosome>> index = new TreeMap<>();

	private static final Map<String, Chromosome> unknown = new HashMap<>();
	private static final List<Chromosome> unknownList = new ArrayList<>();

	static {
		index.put(Chromosome.Namespace.GENEBANK, indexBy(Chromosome::getGeneBank));
		index.put(Chromosome.Namespace.GRCH, indexBy(Chromosome::getName));
		index.put(Chromosome.Namespace.UCSC, indexBy(Chromosome::getUcsc));
		index.put(Chromosome.Namespace.REFSEQ, indexBy(Chromosome::getRefseq));
	}

	private static Map<String, Chromosome> indexBy(Function<Chromosome, String> key) {
		final Map<String, Chromosome> map = new TreeMap<>();
		for (Chromosome chromosome : chromosomes) {
			final String k = key.apply(chromosome);
			if (k.equals(NA)) continue;
			map.put(k, chromosome);
		}
		return Collections.unmodifiableMap(map);
	}

	private final String name;
	private final String role;
	private final String molecule;
	private final String type;
	private final String geneBank;
	private final String relationship;
	private final String refseq;
	private final String assemblyUnit;
	private final long length;
	private final String ucsc;

	Chromosome(String name, String role, String molecule, String type, String geneBank, String relationship, String refseq, String assemblyUnit, long length, String ucsc) {
		this.name = name;
		this.role = role;
		this.molecule = molecule;
		this.type = type;
		this.geneBank = geneBank;
		this.relationship = relationship;
		this.refseq = refseq;
		this.assemblyUnit = assemblyUnit;
		this.length = length;
		this.ucsc = ucsc;
	}

	/**
	 * Gets the name of the chromosome using the GRCH namespace.
	 */
	public String getName() {
		return name;
	}

	public String getName(Namespace namespace) {
		return namespace.getName(this);
	}

	public String getMolecule() {
		return molecule;
	}

	public String getRole() {
		return role;
	}

	public String getType() {
		return type;
	}

	public String getGeneBank() {
		return geneBank;
	}

	public String getRelationship() {
		return relationship;
	}

	String getRefseq() {
		return refseq;
	}

	public String getAssemblyUnit() {
		return assemblyUnit;
	}

	public long getLength() {
		return length;
	}

	public String getUcsc() {
		return ucsc;
	}

	/**
	 * Gets, or creates, the instance of the corresponding chromosome with name under namespace. If
	 * name is not in namespace, tries to find name in other namespaces. If name is not under any
	 * namespace, a new synthetic Chromosome instance will be created under <em>unknown</em>
	 * namespace.
	 *
	 * @param name
	 * 		name of the chromosome
	 * @param namespace
	 * 		namespace for the name
	 * @return best match of the chromosome
	 */
	public static Chromosome get(String name, Chromosome.Namespace namespace) {
		Chromosome chr = index.get(namespace).get(name);
		if (chr != null) return chr;
		// Try to find the chromosome into another namespace
		for (Namespace ns : Namespace.values()) {
			if (ns == namespace) continue;
			chr = index.get(ns).get(name);
			if (chr != null) return chr;
		}
		// Maybe it is an unknown chr
		chr = unknown.get(name);
		if (chr != null) return chr;
		// Create a synthetic chromosome
		chr = new Chromosome(name, NA, NA, NA, name, NA, name, NA, 0, name);
		unknown.put(name, chr);
		unknownList.add(chr);
		return chr;
	}

	/**
	 * Shortcut for {@code Chromosome.get(Namespace.getDefault()}
	 *
	 * @param name
	 * 		chromosome name
	 * @see Chromosome#get(String, Namespace)
	 */
	public static Chromosome get(String name) {
		return index.get(Chromosome.Namespace.getDefault()).get(name);
	}

	@Override
	public int compareTo(Chromosome that) {
		if (this == that) return 0;
		if (chromosomes.contains(this) && chromosomes.contains(that))
			return Integer.compare(chromosomes.indexOf(this), chromosomes.indexOf(that));
		else if (chromosomes.contains(this)) return -1;
		else if (chromosomes.contains(that)) return 1;
		else return Integer.compare(unknownList.indexOf(this), unknownList.indexOf(that));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Chromosome that = (Chromosome) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public enum Namespace {
		GRCH {
			@Override
			public String getName(Chromosome chromosome) {
				return chromosome.getName();
			}
		}, UCSC {
			@Override
			public String getName(Chromosome chromosome) {
				return chromosome.getUcsc();
			}
		}, REFSEQ {
			@Override
			public String getName(Chromosome chromosome) {
				return chromosome.getRefseq();
			}
		}, GENEBANK {
			@Override
			public String getName(Chromosome chromosome) {
				return chromosome.getGeneBank();
			}
		};

		public static Namespace guess(VCFHeader vcfHeader) {
			final Map<Namespace, Integer> guesses = new HashMap<>();
			final Set<String> headerChroms = vcfHeader.getContigLines()
				.stream().map(VCFContigHeaderLine::getID).collect(Collectors.toSet());
			for (Chromosome chromosome : chromosomes) {
				for (Namespace namespace : Namespace.values()) {
					if (headerChroms.contains(namespace.getName(chromosome))) {
						guesses.merge(namespace, 0, Integer::sum);
					}
				}
			}
			// Can't do anything to guess
			if (headerChroms.isEmpty()) return getDefault();
			final Map<Namespace, Integer> sorted = sortByValue(guesses, Comparator.comparing(Integer::intValue).reversed());
			return sorted.entrySet().iterator().next().getKey();
		}

		public abstract String getName(Chromosome chromosome);

		public static Namespace getDefault() {
			return GRCH;
		}
	}

	public static  <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, Comparator<V> comparator) {
		List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
		list.sort(Map.Entry.comparingByValue(comparator));

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}
}
