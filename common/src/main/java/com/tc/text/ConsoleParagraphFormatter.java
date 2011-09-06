/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.text;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ConsoleParagraphFormatter implements ParagraphFormatter {

  private final StringFormatter sf;
  private final int             maxWidth;

  public ConsoleParagraphFormatter(int maxWidth, StringFormatter stringFormatter) {
    this.maxWidth = maxWidth;
    this.sf = stringFormatter;
  }

  public String format(String in) {
    StringBuffer buf = new StringBuffer();
    if (in == null) throw new AssertionError();
    List words = parseWords(in);
    int lineWidth = 0;
    for (Iterator i = words.iterator(); i.hasNext();) {
      String currentWord = (String) i.next();
      if (lineWidth + currentWord.length() > maxWidth) {
        if (lineWidth > 0) {
          buf.append(sf.newline());
        }
        lineWidth = currentWord.length();
      } else {
        if (lineWidth > 0) {
          buf.append(" ");
        }
        lineWidth += currentWord.length();
      }
      buf.append(currentWord);
    }
    return buf.toString();
  }

  private List parseWords(String in) {
    String[] words = in.split("\\s+");
    return Arrays.asList(words);
  }

}
