/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.util;

import java.util.List;
import java.util.ArrayList;

/**
 * Utility methods for strings.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class Strings {
  /**
   * Private constructor to prevent instantiability.
   */
  private Strings() {
  }

  /**
   * Removes newline, carriage return and tab characters from a string.
   *
   * @param toBeEscaped string to escape
   * @return the escaped string
   */
  public static String removeFormattingCharacters(final String toBeEscaped) {
    StringBuffer escapedBuffer = new StringBuffer();
    for (int i = 0; i < toBeEscaped.length(); i++) {
      if ((toBeEscaped.charAt(i) != '\n') && (toBeEscaped.charAt(i) != '\r') && (toBeEscaped.charAt(i) != '\t')) {
        escapedBuffer.append(toBeEscaped.charAt(i));
      }
    }
    String s = escapedBuffer.toString();
    return s;//
    // Strings.replaceSubString(s, "\"", "")
  }

  /**
   * Replaces all occurences of a substring inside a string.
   *
   * @param str      the string to search and replace in
   * @param oldToken the string to search for
   * @param newToken the string to replace newToken
   * @return the new string
   */
  public static String replaceSubString(final String str, final String oldToken, final String newToken) {
    return replaceSubString(str, oldToken, newToken, -1);
  }

  /**
   * Replaces all occurences of a substring inside a string.
   *
   * @param str      the string to search and replace in
   * @param oldToken the string to search for
   * @param newToken the string to replace newToken
   * @param max      maximum number of values to replace (-1 => no maximum)
   * @return the new string
   */
  public static String replaceSubString(final String str, final String oldToken, final String newToken, int max) {
    if ((str == null) || (oldToken == null) || (newToken == null) || (oldToken.length() == 0)) {
      return str;
    }
    StringBuffer buf = new StringBuffer(str.length());
    int start = 0;
    int end = 0;
    while ((end = str.indexOf(oldToken, start)) != -1) {
      buf.append(str.substring(start, end)).append(newToken);
      start = end + oldToken.length();
      if (--max == 0) {
        break;
      }
    }
    buf.append(str.substring(start));
    return buf.toString();
  }

  /**
   * String split on multicharacter delimiter. <p/>Written by Tim Quinn (tim.quinn@honeywell.com)
   *
   * @param stringToSplit
   * @param delimiter
   * @return
   */
  public static final String[] splitString(String stringToSplit, String delimiter) {
    String[] aRet;
    int iLast;
    int iFrom;
    int iFound;
    int iRecords;

    // return Blank Array if stringToSplit == "")
    if (stringToSplit.equals("")) {
      return new String[0];
    }

    // count Field Entries
    iFrom = 0;
    iRecords = 0;
    while (true) {
      iFound = stringToSplit.indexOf(delimiter, iFrom);
      if (iFound == -1) {
        break;
      }
      iRecords++;
      iFrom = iFound + delimiter.length();
    }
    iRecords = iRecords + 1;

    // populate aRet[]
    aRet = new String[iRecords];
    if (iRecords == 1) {
      aRet[0] = stringToSplit;
    } else {
      iLast = 0;
      iFrom = 0;
      iFound = 0;
      for (int i = 0; i < iRecords; i++) {
        iFound = stringToSplit.indexOf(delimiter, iFrom);
        if (iFound == -1) { // at End
          aRet[i] = stringToSplit.substring(iLast + delimiter.length(), stringToSplit.length());
        } else if (iFound == 0) { // at Beginning
          aRet[i] = "";
        } else { // somewhere in middle
          aRet[i] = stringToSplit.substring(iFrom, iFound);
        }
        iLast = iFound;
        iFrom = iFound + delimiter.length();
      }
    }
    return aRet;
  }

  /**
   * Parse a method signature or method call signature.
   * <br/>Given a call signature like "method(Type t)", extract the method name
   * and param type and parameter name: [method, Type, t]
   * <br/>Given a signature like "method(X x, Y)", extract the method name
   * and param name / param type - but leaving empty String if
   * the information is not available: [method, X, x, Y, ""]
   *
   * @param methodCallSignature
   * @return each element (2xp+1 sized) (see doc)
   */
  public static String[] extractMethodSignature(String methodCallSignature) {
    List extracted = new ArrayList();
    String methodName = methodCallSignature;
    String methodCallDesc = null;
    if (methodCallSignature.indexOf("(") > 0) {
      methodName = methodName.substring(0, methodCallSignature.indexOf("("));
      methodCallDesc =
              methodCallSignature.substring(methodCallSignature.indexOf("(") + 1, methodCallSignature.lastIndexOf(")"));
    }
    extracted.add(methodName);
    if (methodCallDesc != null) {
      String[] parameters = Strings.splitString(methodCallDesc, ",");
      for (int i = 0; i < parameters.length; i++) {
        String[] parameterInfo = Strings.splitString(
                Strings.replaceSubString(
                        parameters[i].trim(),
                        "  ",
                        " "
                ), " "
        );
        extracted.add(parameterInfo[0]);
        extracted.add((parameterInfo.length > 1) ? parameterInfo[1] : "");
      }
    }
    return (String[]) extracted.toArray(new String[]{});
  }

  public static boolean isNullOrEmpty(String s) {
    return (s == null) ? true : (s.length() <= 0);
  }
}