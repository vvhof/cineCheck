package cineCheck;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that generates all possible combinations of characters in a give
 * vocabulary
 * 
 * @author vincentvonhof
 */
public class Variants {
	
	public char[] vocabulary;

	/**
	 * Constructor with vocabulary to be used for the variant generation.
	 * 
	 * @param vocabulary vocabulary to be used char basis for variant generation.
	 */
	public Variants(char[] vocabulary) {
		this.vocabulary = vocabulary;
	}

	// iterative for mem savings
	public List<char[]> generateCombinations(int arraySize) {
		int carry;
		int[] indices = new int[arraySize];
		List<char[]> variants = new ArrayList<char[]>();
		
		do {
			char[] singleResult = new char[arraySize];
			int j = 0;
			for (int index : indices) {
				singleResult[j++] = vocabulary[index];
			}
			variants.add(singleResult);

			carry = 1;
			for (int i = indices.length - 1; i >= 0; i--) {
				if (carry == 0)
					break;

				indices[i] += carry;
				carry = 0;

				if (indices[i] == vocabulary.length) {
					carry = 1;
					indices[i] = 0;
				}
			}
		} while (carry != 1); // Call this method iteratively until a carry is
								// left over
		return variants;
	}
	
}
