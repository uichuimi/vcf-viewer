package org.uichuimi.variant.viewer.utils;

import java.util.StringJoiner;

/**
 * Methods extracted from BitSet, to allow working with plain long arrays.
 */
public class BitUtils {
	private static final int ADDRESS_BITS_PER_WORD = 6;
	private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
	private static final long WORD_MASK = 0xffffffffffffffffL;

	/**
	 * Sets the bit at position bit to true
	 *
	 * @param words array of words representing a bit set
	 * @param bit   position og the bit to be set to true
	 */
	public static void set(long[] words, int bit) {
		final int word = wordIndex(bit);
		words[word] |= (1L << bit);
	}

	/**
	 * Computes the word corresponding to the bit in bitIndex
	 *
	 * @param bitIndex position of the bit
	 * @return index of the word
	 */
	private static int wordIndex(int bitIndex) {
		return bitIndex >> ADDRESS_BITS_PER_WORD;
	}

	/**
	 * Sets the bit at position bit to false
	 *
	 * @param words array of words representing a bit set
	 * @param bit   position og the bit to be set to false
	 */
	public static void clear(long[] words, int bit) {
		final int word = wordIndex(bit);
		words[word] &= ~(1L << bit);
	}

	/**
	 * Computes the intersection of 2 bitsets
	 *
	 * @param a one of the bitsets
	 * @param b the other bitset
	 */
	public static boolean intersects(long[] a, long[] b) {
		for (int w = 0; w < a.length; w++) {
			if ((a[w] & b[w]) != 0) return true;
		}
		return false;
	}

	/**
	 * Creates  the string representation of the bitset, by printing the indices of the true values.
	 * <p>Example:</p>
	 * <pre>  {0,4,7,9}</pre>
	 * represents the bitset:
	 * <pre>  1000100101</pre>
	 *
	 * @param words a bitset
	 * @return the string representation of the bitset
	 */
	public static String toString(long[] words) {
		final StringJoiner rtn = new StringJoiner(",", "{", "}");
		for (int bit = 0; bit < words.length * BITS_PER_WORD; bit++) {
			if (get(words, bit)) rtn.add(String.valueOf(bit));
		}
		return rtn.toString();
	}

	public static boolean get(long[] words, int bit) {
		int wordIndex = wordIndex(bit);
		return (words[wordIndex] & 1L << bit) != 0;
	}


	public static void flip(final long[] mask) {
		for (int i = 0; i < mask.length; i++) {
			mask[i] ^= WORD_MASK;
		}
	}
}
