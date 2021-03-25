package org.uichuimi.variant.viewer.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.MissingResourceException;
import java.util.stream.Collectors;

public class ChromosomeFactory {

	// Load the list of chromosomes from resource file
	private static final String REPORT = "GCF_000001405.39_GRCh38.p13_assembly_report.txt";

	// Ensure only one list is loaded per execution
	private static List<Chromosome> chromosomes;

	public static List<Chromosome> getChromosomeList() {
		if (chromosomes == null) chromosomes = loadFromResource();
		return chromosomes;
	}

	private static List<Chromosome> loadFromResource() {
		final InputStream resource = ChromosomeFactory.class.getResourceAsStream(ChromosomeFactory.REPORT);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
			return reader.lines()
				.filter(line -> !line.startsWith("#"))
				.map(ChromosomeFactory::create)
				.collect(Collectors.toList());
		} catch (IOException e) {
			throw new MissingResourceException("Missing assembly report resource", ChromosomeFactory.class.getName(), REPORT);
		}
	}

	private static Chromosome create(String line) {
		final String[] fields = line.split("\t");
		final String name = fields[0];
		final String role = fields[1];
		final String molecule = fields[2];
		final String type = fields[3];
		final String geneBank = fields[4];
		final String relationship = fields[5];
		final String refseq = fields[6];
		final String assemblyUnit = fields[7];
		final long length = Long.parseLong(fields[8]);
		final String ucsc = fields[9];
		return new Chromosome(name, role, molecule, type, geneBank, relationship, refseq, assemblyUnit, length, ucsc);
	}

}
