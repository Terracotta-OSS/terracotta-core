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
package com.tc.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReplaceLine {

  private ReplaceLine() {
    // cannot instantiate
  }

  /**
   * Replaces lines matching a token regular expression group.
   * 
   * @return true if all tokens found matches
   */
  @FindbugsSuppressWarnings("DM_DEFAULT_ENCODING")
  public static void parseFile(ReplaceLine.Token[] tokens, File file) throws FileNotFoundException, IOException {
    Arrays.sort(tokens, new Comparator() {
      @Override
      public int compare(Object o1, Object o2) {
        return Integer.valueOf(((Token) o1).lineNumber).compareTo(Integer.valueOf(((Token) o2).lineNumber));
      }
    });

    int tokenIndex = 0, lineIndex = 0;
    StringBuffer text = new StringBuffer();
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        if (tokenIndex < tokens.length && ++lineIndex == tokens[tokenIndex].lineNumber) {
          line = replaceToken(tokens[tokenIndex].replacePattern, line, tokens[tokenIndex].value);
          tokenIndex++;
        }
        text.append(line + "\n");
      }
    } finally {
      try {
        reader.close();
      } catch (IOException ioe) {
        // ignore
      }
    }

    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));

    try {
      out.write(text.toString().getBytes());
      out.flush();
    } finally {
      try {
        out.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  private static String replaceToken(String expression, String text, String value) {
    Pattern pattern = Pattern.compile(expression);
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      return matcher.replaceAll(value);
    }
    return text;
  }

  public static class Token {
    private final int    lineNumber;
    private final String replacePattern;
    private final String value;

    public Token(int lineNumber, String replacePattern, String value) {
      this.lineNumber = lineNumber;
      this.replacePattern = replacePattern;
      this.value = value;
    }
  }
}
