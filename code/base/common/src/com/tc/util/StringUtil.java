/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.Arrays;

public class StringUtil {

  public static final char   SPACE        = ' ';
  public static final String SPACE_STRING = " ";

  public static final String EMPTY        = "";

  public static final String NULL_STRING  = "<null>";

  public static final boolean isNullOrBlank(String s) {
    return s == null || EMPTY.equals(s.trim());
  }

  public static final String safeToString(Object object) {
    return object != null ? object.toString() : NULL_STRING;
  }

  /**
   * @param list a list of String objects
   * @param value a string to search for, may be null -- will return true if one of the array values in
   *        <code>list</code> is null.
   * @return true if <code>value</code> is contained in <code>list</code>, will search for <code>null</code> as
   *         well.
   */
  public static final boolean exists(String[] list, String value) {
    if (list != null) {
      for (int pos = 0; pos < list.length; ++pos) {
        if (list[pos] == value || (list[pos] != null && value != null && list[pos].equals(value))) { return true; }
      }
    }
    return false;
  }

  /**
   * @return <code>position</code>th, <code>position</code>st, <code>position</code>nd, etc. - whatever is
   *         appropriate.
   */
  public static String ordinal(long position) {
    long mod10 = Math.abs(position) % 10;
    long mod100 = Math.abs(position) % 100;
    StringBuffer rv = new StringBuffer(EMPTY + position);
    if (mod10 == 1) {
      rv.append(mod100 == 11 ? "th" : "st");
    } else if (mod10 == 2) {
      rv.append(mod100 == 12 ? "th" : "nd");
    } else if (mod10 == 3) {
      rv.append(mod100 == 13 ? "th" : "rd");
    } else {
      rv.append("th");
    }
    return rv.toString();
  }

  public static String indentLines(String source) {
    return indentLines(source, 1);
  }

  public static String indentLines(String source, int indentLevel) {
    return indentLines(new StringBuffer(source), indentLevel).toString();
  }

  public static StringBuffer indentLines(StringBuffer source, int indentLevel) {
    return indentLines(source, indentLevel, '\t');
  }

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

  public static int indexOfStringBuffer(StringBuffer source, String search, int start) {
    return source.toString().indexOf(search, start);
  }

  /**
   * Creates a String representation of an array of objects by calling <code>toString</code> on each one. Formatting
   * is controlled by the parameters.
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
  public static final String toString(Object[] objs) {
    return toString(objs, ", ", null, null);
  }

  public static final String toString(int[] objs, String separator, String prefix, String postfix) {
    StringBuffer rv = new StringBuffer();
    if (objs != null) {
      for (int pos = 0; pos < objs.length; ++pos) {
        if (rv.length() > 0 && separator != null) {
          rv.append(separator);
        }
        if (prefix != null) {
          rv.append(prefix);
        }
        rv.append(objs[pos]);
        if (postfix != null) {
          rv.append(postfix);
        }
      }
    } else {
      rv.append(NULL_STRING);
    }
    return rv.toString();
  }

  public static final String toPaddedString(long value, int radix, int paddedWidth) {
    StringBuffer result = new StringBuffer();
    String strValue = Long.toString(value, radix);
    for (int pos = 0; pos < paddedWidth - strValue.length(); ++pos) {
      result.append("0");
    }
    result.append(strValue);
    return result.toString();
  }

  public static final String rightJustify(String s, int fieldSize) {
    if (s.length() == fieldSize) return s;
    if (s.length() > fieldSize) {
      final int i = Math.max(s.length() - (fieldSize - 3), 1);
      return leftPad(s.substring(i), fieldSize, '.');
    }
    return leftPad(s, fieldSize, ' ');
  }

  public static final String rightPad(String s, int size, char padChar) {
    if (s.length() >= size) return s;
    final int padCount = size - s.length();
    StringBuffer sb = new StringBuffer();
    sb.append(s);
    for (int i = 0; i < padCount; i++) {
      sb.append(padChar);
    }
    String rv = sb.toString();
    Assert.eval(rv.length() == size);
    return rv;
  }

  public static final String leftPad(String s, int size, char padChar) {
    if (s.length() >= size) return s;
    final int padCount = size - s.length();
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < padCount; i++) {
      sb.append(padChar);
    }
    sb.append(s);
    String rv = sb.toString();
    Assert.eval(rv.length() == size);
    return rv;
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
   * @return a modified version of <code>source</code>, or null if <code>source</code> or <code>search</code> are
   *         null
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
   * Reduces the size that a string occupies to the minimal possible by 
   * ensuring that the back-end char array contains exactly the characters that
   * are needed, and no more.
   * 
   * Note that this method doesn't modify the original string as they are
   * immutable, a new string is returned instead.
   * 
   * @param source the string that needs to be reduced
   * @return the reduces string
   */
  public static final String reduce(String source) {
    if (null == source) return null;
    
    char[] chars = new char[source.length()];
    source.getChars(0, source.length(), chars, 0);
    return new String(chars);
  }

  public static final String getNonNull(String s, String nullToken) {
    return (s == null) ? nullToken : s;
  }

  public static final String getNonNull(String s) {
    return getNonNull(s, "");
  }

}
