package org.uichuimi.variant.viewer.utils;

import htsjdk.variant.variantcontext.GenotypeType;

import java.util.List;
import java.util.Map;

public class Constants {

	public static final String REF = "REF";
	public static final String FILTER = "FILTER";
	public static final String ID = "ID";
	public static final String QUAL = "QUAL";
	public static final String POS = "POS";
	public static final String CHROM = "CHROM";
	public static final String ALT = "ALT";
	public static final String EMPTY_CELL = "-";

	private static final Map<GenotypeType, GenotypeType> validGenotypeTypes = Map.of(
		GenotypeType.UNAVAILABLE, GenotypeType.NO_CALL,
		GenotypeType.MIXED, GenotypeType.NO_CALL
	);

	private Constants() {
	}

	/**
	 * This map simplifies the enum of {@link GenotypeType} to only 4 options: {@link GenotypeType#NO_CALL},
	 * {@link GenotypeType#HOM_REF}, {@link GenotypeType#HET}, {@link GenotypeType#HOM_VAR}. Other options are
	 * mapped to {@link GenotypeType#NO_CALL}.
	 */
	public static GenotypeType validGenotypeType(GenotypeType type) {
		return validGenotypeTypes.getOrDefault(type, type);
	}

	/**
	 *
	 * @return a list with the valid genotype types in order.
	 */
	public static List<GenotypeType> validGenotypeTypes() {
		return List.of(GenotypeType.NO_CALL, GenotypeType.HOM_REF, GenotypeType.HET, GenotypeType.HOM_VAR);
	}
}
