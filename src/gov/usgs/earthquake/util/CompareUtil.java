package gov.usgs.earthquake.util;


public class CompareUtil {

	/**
	 * A method to simplify comparison of two values, either of which may be
	 * null.
	 * 
	 * For purposes of this comparison, null values are &gt; non-null values.
	 * 
	 * @param a
	 *            value to compare
	 * @param b
	 *            value to compare
	 * @return -1, if a is not null and b is null; 0, if a is null and b is
	 *         null; 1, if a is null and b is not null; otherwise,
	 *         a.compareTo(b).
	 * @see #accept(IndexerEvent, IndexerChange)
	 */
	public static <T extends Comparable<T>> int nullSafeCompare(final T a, final T b) {
		if (a == null && b != null) {
			// null > real values
			return 1;
		} else if (a != null && b == null) {
			// real values < null
			return -1;
		} else if (a == null && b == null) {
			// null == null
			return 0;
		} else {
			// not null, use object compareTo
			return a.compareTo(b);
		}
	}

}
