package org.uichuimi.variant.viewer.index;

import htsjdk.variant.variantcontext.GenotypeType;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GenotypeSearchEngine implements Serializable {

	@Serial
	private static final long serialVersionUID = 5479357311104154358L;
	private final List<String> samples;
	private final long[] index;
	private final int numberOfWords;
	private final int size;

	public GenotypeSearchEngine(long[] index, Collection<String> samples, int numberOfWords) {
		this.index = index;
		this.samples = new ArrayList<>(samples);
		this.numberOfWords = numberOfWords;
		this.size = index.length / numberOfWords;
	}

	public boolean query(int position, List<Clause> query) {
		List<long[]> maskList = new ArrayList<>();
		for (Clause clause : query) {
			maskList.add(toMask(clause));
		}
		for (int m = 0; m < maskList.size(); m++) {
			long[] mask = maskList.get(m);
			int minMatch = query.get(m).getMinMatch();
			final int matches = BitUtils.intersection(index, mask, position * numberOfWords, 0, numberOfWords);
			if (matches < minMatch) {
				return false;
			}
		}
		return true;
	}

	public List<Long> query(List<Clause> query) {

		final List<Long> rtn = new ArrayList<>();

		List<long[]> maskList = new ArrayList<>();
		for (Clause clause : query) {
			maskList.add(toMask(clause));
		}

		for (int vid = 0; vid < size; vid++) {

			Boolean toAdd = true;
			for (int j = 0; j < maskList.size(); j++) {
				long[] mask = maskList.get(j);
				int minMatch = query.get(j).getMinMatch();
				final int matches = BitUtils.intersection(index, mask, vid * numberOfWords, 0, numberOfWords);
				if (matches < minMatch) {
					toAdd = false;
					break;
				}
			}
			if (toAdd) {
				rtn.add((long) vid);
			}
		}

		return rtn;
	}

	private long[] toMask(Clause queryObject) {
		final long[] mask = new long[numberOfWords];

		queryObject.getSamples().forEach((person) -> {
			final int offset = GenotypeType.values().length * samples.indexOf(person);
			for (GenotypeType type : GenotypeType.values()) {
				int pos = type.ordinal();
				if (queryObject.getTypes().contains(type)) {
					BitUtils.set(mask, offset + pos);
				}
			}
		});

		return mask;
	}

}
