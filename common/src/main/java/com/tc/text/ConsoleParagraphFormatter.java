/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
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
