/*
 * StringUtils
 *
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * String parsing and utility functions.
 */
public class StringUtils {

    /**
     * No Exception Double parsing method.
     * 
     * @param value
     * @return null on error, otherwise Double value.
     */
    public static Double getDouble(final String value) {
        Double d;
        try {
            d = Double.valueOf(value);
        } catch (Exception e) {
            d = null;
        }
        return d;
    }

    /**
     * No Exception Float parsing method.
     * 
     * @param value
     * @return null on error, otherwise Float value.
     */
    public static Float getFloat(final String value) {
        Float f;
        try {
            f = Float.valueOf(value);
        } catch (Exception e) {
            f = null;
        }
        return f;
    }

    /**
     * No Exception Integer parsing method.
     * 
     * @param value
     * @return null on error, otherwise Integer value.
     */
    public static Integer getInteger(final String value) {
        Integer i;
        try {
            i = Integer.valueOf(value);
        } catch (Exception e) {
            i = null;
        }
        return i;
    }

    /**
     * No Exception Long parsing method.
     * 
     * @param value
     * @return null on error, otherwise Integer value.
     */
    public static Long getLong(final String value) {
        Long l;
        try {
            l = Long.valueOf(value);
        } catch (Exception e) {
            l = null;
        }
        return l;
    }

    /**
     * Join objects in a list using the specified delimiter. The objects
     * toString method is used to get a string value.
     * 
     * @param delimiter
     *            string to insert between list items.
     * @param list
     *            items to join.
     * @return string containing delimiter delimited list items.
     */
    public static String join(final List<Object> list, final String delimiter) {
        if (list == null || list.size() == 0) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        Iterator<Object> iter = list.iterator();
        // add first element to buffer
        if (iter.hasNext()) {
            buf.append(iter.next().toString());
        }
        // add delimiter before other elements
        while (iter.hasNext()) {
            buf.append(delimiter);
            buf.append(iter.next().toString());
        }

        return buf.toString();
    }

    /**
     * Split a string into a list of strings using the specified delimiter. The
     * intrinsic String.split method is used and elements of the returned String
     * array (if any) are added to a list.
     * 
     * @param toSplit
     *            string to split.
     * @param delimiter
     *            string used to separate items in toSplit
     * @return a list containing items.
     */
    public static List<String> split(final String toSplit,
            final String delimiter) {
        List<String> list = new LinkedList<String>();
        if (toSplit != null) {
            String[] array = toSplit.split(delimiter);
            for (String value : array) {
                value = value.trim();
                if (!"".equals(value)) {
                    list.add(value);
                }
            }
        }
        return list;
    }

}
