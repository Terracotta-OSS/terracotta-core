/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.Arrays;

/**
 * String utility methods.
 */
public class StringUtil {

  /** A space character */
  public static final char   SPACE          = ' ';

  /** A space string */
  public static final String SPACE_STRING   = " ";

  /** The empty string */
  public static final String EMPTY          = "";

  /** A string representing a null value: "<null>" */
  public static final String NULL_STRING    = "<null>";

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * Normal toString(), but convert null to the {@link #NULL_STRING}.
   * 
   * @return Always a string, never null
   */
  public static final String safeToString(Object object) {
    return object != null ? object.toString() : NULL_STRING;
  }

  /**
   * Indent lines using a single tab by inserting them into source after line breaks and returning a new String.
   * 
   * @param source Source string, may NOT be null
   * @return New string, updated with indents
   * @throws NullPointerException If source is null
   */
  public static String indentLines(String source) {
    return indentLines(source, 1);
  }

  /**
   * Indent lines using tabs by inserting them into source after line breaks and returning a new String.
   * 
   * @param source Source string, may NOT be null
   * @param indentLevel Number of tabs to insert, must be >= 0
   * @return Original buffer, updated
   * @throws IllegalArgumentException If indentLevel < 0
   * @throws NullPointerException If source is null
   */
  public static String indentLines(String source, int indentLevel) {
    return indentLines(new StringBuffer(source), indentLevel).toString();
  }

  /**
   * Indent lines using tabs by inserting them into source and returning source.
   * 
   * @param source Source buffer, may be null
   * @param indentLevel Number of tabs to insert, must be >= 0
   * @return Original buffer, updated
   * @throws IllegalArgumentException If indentLevel < 0
   */
  public static StringBuffer indentLines(StringBuffer source, int indentLevel) {
    return indentLines(source, indentLevel, '\t');
  }

  /**
   * Indent lines in the StringBuffer (line breaks found at \n) with indentChar repeated indentLevel times.
   * 
   * @param source Source buffer, may be null
   * @param indentLevel Number of chars to indent, must be >= 0
   * @param indentChar Indent character (usually ' ' or '\t')
   * @return Original buffer, updated
   * @throws IllegalArgumentException If indentLevel < 0
   */
  public static StringBuffer indentLines(StringBuffer source, int indentLevel, char indentChar) {
    if ((source == null) || (indentLevel == 0)) { return source; }

    final String indentStr;

    if (indentLevel <= 0) {
      // I suppose one could write something to remove indentation, but I don't feel like it now
      throw new IllegalArgumentException("Negative indentation not supported");
    } else if (indentLevel > 1) {
      char[] chars = new char[indentLevel];
      Arrays.fill(chars, indentChar);
      indentStr = new String(chars);
    } else {
      // errr....the call below is jdk1.4 specific
      // indentStr = Character.toString(indentChar);

      indentStr = new String(new char[] { indentChar });
    }

    source.insert(0, indentStr);

    int index = 0;
    while ((index = indexOfStringBuffer(source, "\n", index)) != -1) {
      index++;
      if (index == source.length()) {
        break;
      }
      source.insert(index, indentStr);
    }

    return source;
  }

  /**
   * Find index of "search" in "source", starting at "start" index.
   * 
   * @param source Source buffer, must be non-null
   * @param search Search string, must be non-null
   * @param start Start index, should be 0<=start<source.length(), will return -1 if out of range
   * @return Index of found string or -1 if not found
   * @throws NullPointerException If source or search is null
   */
  public static int indexOfStringBuffer(StringBuffer source, String search, int start) {
    return source.toString().indexOf(search, start);
  }

  /**
   * Creates a String representation of an array of objects by calling <code>toString</code> on each one. Formatting is
   * controlled by the parameters.
   * 
   * @param objs (required) the array of objects to display
   * @param separator (optional) a string to place between each object
   * @param prefix (optional) a string to prefix each object with
   * @param postfix (optional) a string to append to each object
   * @return a String representation of the array, never returns null
   */
  public static final String toString(Object[] objs, String separator, String prefix, String postfix) {
    final String sep = getNonNull(separator);
    final String pre = getNonNull(prefix);
    final String post = getNonNull(postfix);
    StringBuffer rv = new StringBuffer();
    if (objs != null) {
      for (int pos = 0; pos < objs.length; ++pos) {
        if (rv.length() > 0) {
          rv.append(sep);
        }
        rv.append(pre);
        rv.append(objs[pos] != null ? objs[pos].toString() : "null");
        rv.append(post);
      }
    } else {
      rv.append(NULL_STRING);
    }
    return rv.toString();
  }

  /**
   * Helper method to convert object array [a, b, c] to comma-separated string "a, b, c".
   * 
   * @param objs Array of objects, can be null
   * @return String, never null
   */
  public static final String toString(Object[] objs) {
    return toString(objs, ", ", null, null);
  }

  /**
   * Format value to string using radix, then prepend with 0's out to paddedWidth. If the formatted value is >
   * paddedWidth, then the value is returned.
   * 
   * @param value Long value, must be >= 0
   * @param radix The radix to use when representing the value
   * @param paddedWidth The width to pad to by prepending 0
   * @return Padded formatted string value for the long value, never null
   */
  public static final String toPaddedString(long value, int radix, int paddedWidth) {
    StringBuffer result = new StringBuffer();
    String strValue = Long.toString(value, radix);
    for (int pos = 0; pos < paddedWidth - strValue.length(); ++pos) {
      result.append("0");
    }
    result.append(strValue);
    return result.toString();
  }

  /**
   * Simple search/replace for non-pattern strings. optionally skipping over quoted strings.
   * 
   * @param source the original string to perform the search/replace on, a modified version of this is returned, if null
   *        then null will be returned immediately
   * @param search the string to search for in <code>source</code>, if null then null is returned immediately
   * @param replace the string to replace <code>search</code> occurrences in <code>source</code>, if null then the
   *        search string is simply removed from source and not replaced with anything
   * @param skipQuotedStrings if true then quoted strings will be skipped over
   * @return a modified version of <code>source</code>, or null if <code>source</code> or <code>search</code> are null
   */
  public static final String replaceAll(String source, String search, String replace, boolean skipQuotedStrings) {
    if (source == null || search == null) { return null; }
    StringBuffer result = new StringBuffer();
    int beginQuoteIdx = 0;
    for (int pos = 0; pos < source.length(); ++pos) {
      if (skipQuotedStrings && source.startsWith("'", pos)) {
        // Skip the single-quoted string
        beginQuoteIdx = pos;
        for (++pos; pos < source.length() && !source.startsWith("'", pos); ++pos) {
          // skip
        }

        result.append(source.substring(beginQuoteIdx, pos + 1));
      } else if (skipQuotedStrings && source.startsWith("\"", pos)) {
        // Skip the double-quoted string
        beginQuoteIdx = pos;
        for (++pos; pos < source.length() && !source.startsWith("\"", pos); ++pos) {
          // skip
        }
        result.append(source.substring(beginQuoteIdx, pos + 1));
      } else if (source.startsWith(search, pos)) {
        if (replace != null) {
          result.append(replace);
        }
        pos += search.length() - 1;
      } else {
        result.append(source.charAt(pos));
      }
    }
    return result.toString();
  }

  /**
   * Reduces the size that a string occupies to the minimal possible by ensuring that the back-end char array contains
   * exactly the characters that are needed, and no more. Note that this method doesn't modify the original string as
   * they are immutable, a new string is returned instead.
   * 
   * @param source the string that needs to be reduced
   * @return the reduced string, null if source is null
   */
  public static final String reduce(String source) {
    if (null == source) return null;

    char[] chars = new char[source.length()];
    source.getChars(0, source.length(), chars, 0);
    return new String(chars);
  }

  /**
   * For a string s, if non-null return s, else return nullToken.
   * 
   * @param s The starting string
   * @param nullToken The null token
   * @return s or nullToken depending on s
   */
  public static final String getNonNull(String s, String nullToken) {
    return (s == null) ? nullToken : s;
  }

  /**
   * Get a non-null version of the String.
   * 
   * @param s The string
   * @return Either s or the empty string if s was null
   */
  public static final String getNonNull(String s) {
    return getNonNull(s, EMPTY);
  }
}
