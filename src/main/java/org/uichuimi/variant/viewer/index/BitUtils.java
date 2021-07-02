package org.uichuimi.variant.viewer.index;

import java.util.StringJoiner;

/**
 * Bit operations to work with bit sets. This class contains the same methods as Java {@link
 * java.util.BitSet}, but they apply directly to long[]. This avoids encapsulation, but allows only
 * fixed size bit sets.
 */
public class BitUtils {

	private static final int ADDRESS_BITS_PER_WORD = 6;
	private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

	/**
	 * Sets one bit to 1
	 *
	 * @param words bit set
	 * @param bit   index of bit to set
	 */
	static void set(long[] words, int bit) {
		final int word = wordIndex(bit);
		words[word] |= (1L << bit);
	}

	/**
	 * Sets one bit to 1
	 *
	 * @param words  bit set
	 * @param bit    index of bit to set
	 * @param offset base position of the bit set
	 */
	static void set(long[] words, int bit, int offset) {
		final int word = wordIndex(bit, offset);
		words[word] |= (1L << bit);
	}

	/**
	 * Sets one bit to 0.
	 *
	 * @param words bit set
	 * @param bit   index of bit to set
	 */
	static void clear(long[] words, int bit) {
		final int word = wordIndex(bit);
		words[word] &= ~(1L << bit);
	}

	/**
	 * Sets one bit to 0.
	 *
	 * @param words bit set
	 * @param bit   index of bit to set
	 */
	static void clear(long[] words, int bit, int offset) {
		final int word = wordIndex(bit, offset);
		words[word] &= ~(1L << bit);
	}

	static boolean intersects(long[] a, long[] b) {
		for (int w = 0; w < a.length; w++) {
			if ((a[w] & b[w]) != 0) return true;
		}
		return false;
	}

	/**
	 * Computes how many bits intersect between a and b.
	 *
	 * @param a first bit set
	 * @param b second bit set
	 *
	 * @return number of bits that intersect
	 */
	static int intersection(long[] a, long[] b) {
		int n = 0;
		for (int w = 0; w < a.length; w++) {
			n += Long.bitCount(a[w] & b[w]);
		}
		return n;
	}

	/**
	 * Computes how many bits intersect between a and b.
	 *
	 * @param a first bit set
	 * @param b second bit set
	 *
	 * @return number of bits that intersect
	 */
	static int intersection(long[] a, long[] b, int offsetA, int offsetB, int length) {
		int n = 0;
		for (int w = 0; w < length; w++) {
			n += Long.bitCount(a[offsetA + w] & b[offsetB + w]);
		}
		return n;
	}


	public static boolean get(long[] words, int bit) {
		int wordIndex = wordIndex(bit);
		return (words[wordIndex] & 1L << bit) != 0;
	}

	public static boolean get(long[] words, int bit, int offset) {
		int wordIndex = wordIndex(bit, offset);
		return (words[wordIndex] & 1L << bit) != 0;
	}

	public static String toString(long[] words, int offset, int length) {
		final StringJoiner rtn = new StringJoiner(",", "{", "}");
		for (int bit = 0; bit < length * BITS_PER_WORD; bit++) {
			if (get(words, bit, offset)) rtn.add(String.valueOf(bit));
		}
		return rtn.toString();
	}

	private static int wordIndex(int bitIndex) {
		return bitIndex >> ADDRESS_BITS_PER_WORD;
	}

	private static int wordIndex(int bitIndex, int offset) {
		return (bitIndex >> ADDRESS_BITS_PER_WORD) + offset;
	}

}
