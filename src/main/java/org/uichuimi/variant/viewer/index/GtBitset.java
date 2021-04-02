package org.uichuimi.variant.viewer.index;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

public class GtBitset implements Serializable {

	@Serial
	private final static long serialVersionUID = 2174957617411699716L;
	private final String chromosome;
	private final int start;
	private final int end;
	private final Map<Integer, long[]> bitsets;

	public GtBitset(String chromosome, int start, int end, Map<Integer, long[]> bitsets) {
		this.chromosome = chromosome;
		this.start = start;
		this.end = end;
		this.bitsets = bitsets;
	}

	public String getChromosome() {
		return chromosome;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	/**
	 * @param position genomic position
	 *
	 * @return
	 */
	public long[] getBitset(int position) {
		return bitsets.get(position - start);
	}

}
