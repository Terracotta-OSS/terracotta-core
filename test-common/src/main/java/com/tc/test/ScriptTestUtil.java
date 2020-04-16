/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package com.tc.test;

import java.io.PrintStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common methods for main classes called by script testing.
 */
public final class ScriptTestUtil {
  private static final String BEGIN_FORMAT = "----- BEGIN %S-----";
  private static final String END_FORMAT = "----- END %S-----";

  public static final String ENVIRONMENT_BEGIN_TAG = String.format(BEGIN_FORMAT,"ENVIRONMENT");
  public static final String ENVIRONMENT_END_TAG = String.format(END_FORMAT,"ENVIRONMENT");

  public static final String PROPERTIES_BEGIN_TAG = String.format(BEGIN_FORMAT, "PROPERTIES");
  public static final String PROPERTIES_END_TAG = String.format(END_FORMAT, "PROPERTIES");

  public static final String ARGUMENTS_BEGIN_TAG = String.format(BEGIN_FORMAT, "ARGUMENTS");
  public static final String ARGUMENTS_END_TAG = String.format(END_FORMAT, "ARGUMENTS");

  private static final Pattern LINE_SPLIT = Pattern.compile("\\r?\\n");
  private static final Pattern NAME_VALUE_PARSE = Pattern.compile("([^=]+)=(.*)");

  private ScriptTestUtil() {
    // Prevent instantiation
  }

  /**
   * Writes the environment variables, in order by variable name, to the {@code PrintStream} provided.
   * @param printStream the {@code PrintStream} to which the variables are written
   */
  public static void showEnvironment(PrintStream printStream) {
    printStream.println(ENVIRONMENT_BEGIN_TAG);
    System.getenv().entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(e -> printStream.format("%s=%s%n", e.getKey(), e.getValue()));
    printStream.println(ENVIRONMENT_END_TAG);
  }

  /**
   * Extracts the environment map from the {@link #showEnvironment} string provided.
   * Environment lines not recognized as {@code name=value} pairs are not returned.
   * @param environment the string containing the {@code showEnvironment} output
   * @return a {@code Map} containing the environment values
   */
  public static Map<String, String> extractEnvironment(String environment) {
    return extractMap(environment, true, ENVIRONMENT_BEGIN_TAG, ENVIRONMENT_END_TAG);
  }

  /**
   * Writes the system properties, in order by property name, to the {@code PrintStream} provided.
   * C0 control values appearing in the values are converted to Java escape sequences.
   * @param printStream the {@code PrintStream} to which the properties are written
   * @see #escape(String)
   */
  public static void showProperties(PrintStream printStream) {
    printStream.println(PROPERTIES_BEGIN_TAG);
    System.getProperties().entrySet().stream()
        .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey().toString(), e.getValue().toString()))
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(e -> printStream.format("%s=%s%n", e.getKey(), escape(e.getValue())));
    printStream.println(PROPERTIES_END_TAG);
  }

  /**
   * Extracts the properties map from the {@link #showProperties} string provided.
   * Property lines not recognized as {@code name=value} pairs are not returned.
   * @param properties the string containing the {@code showProperties} output
   * @return a {@code Map} containing the properties values
   */
  public static Map<String, String> extractProperties(String properties) {
    return extractMap(properties, false, PROPERTIES_BEGIN_TAG, PROPERTIES_END_TAG);
  }

  /**
   * Writes the command line arguments to the {@code PrintStream} provided.
   * @param printStream the {@code PrintStream} to which the arguments are written
   */
  public static void showArguments(PrintStream printStream, String[] args) {
    printStream.println(ARGUMENTS_BEGIN_TAG);
    for (int i = 0; i < args.length; i++) {
      printStream.format("[%d]=%s%n", i, args[i]);
    }
    printStream.println(ARGUMENTS_END_TAG);
  }

  /**
   * Extracts the argument list from the {@link #showArguments} string provided.
   * @param arguments the string containing the {@code showArguments} output
   * @return an array containing the argument values
   */
  public static String[] extractArguments(String arguments) {
    Map<String, String> argumentsMap = extractMap(arguments, false, ARGUMENTS_BEGIN_TAG, ARGUMENTS_END_TAG);
    return argumentsMap.values().toArray(new String[0]);
  }

  /**
   * Generates a copy of a string value with characters in the <i>C0 Controls</i> set converted to
   * the appropriate Java escape sequence.  The quote {@code "}, backslash {@code \}, and apostrophe
   * {@code '} are <b>not</b> escaped.
   * @param value the value to convert
   * @return {@code value} with C0 control characters converted to escape sequences
   */
  public static String escape(String value) {
    if (value == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder(value.length());
    StringCharacterIterator sci = new StringCharacterIterator(value);
    for (char current = sci.first(); current != CharacterIterator.DONE; current = sci.next()) {
      if (current <= 0x1F) {
        sb.append('\\');
        switch (current) {
          case 0x08: sb.append('b'); break;
          case 0x09: sb.append('t'); break;
          case 0x0A: sb.append('n'); break;
          case 0x0C: sb.append('f'); break;
          case 0x0D: sb.append('r'); break;
          default:
            sb.append(Integer.toOctalString(current));
        }
      } else {
        sb.append(current);
      }
    }
    return sb.toString();
  }

  /**
   * Extracts a map of the {@code name=value} pairs between the {@code begin} and {@code end} markers.
   * @param environment the string holding the output of the {@link #showEnvironment}, {@link #showProperties},
   *                    and/or {@link #showArguments} methods
   * @param suppressSpecials if {@code true}, suppresses processing lines beginning with
   *                         {@code =}; under Windows, environment variables beginning with {@code =}
   *                         are internal values
   * @param begin the line value marking the beginning of the "show" content to extract
   * @param end the line value marking the ending of the "show" content to extract
   * @return a new {@code Map}, in encounter order, of the values extracted
   */
  private static Map<String, String> extractMap(String environment, boolean suppressSpecials, String begin, String end) {
    Map<String, String> envMap = new LinkedHashMap<>();
    boolean foundBegin = false;
    boolean foundEnd = false;
    Iterator<String> lines = LINE_SPLIT.splitAsStream(environment).iterator();
    while (lines.hasNext()) {
      String line = lines.next();
      if (foundBegin) {
        if (end.equals(line)) {
          foundEnd = true;
          break;
        } else {
          if (suppressSpecials && line.charAt(0) == '=') {
            continue;   // skip this line
          }
          Matcher matcher = NAME_VALUE_PARSE.matcher(line);
          if (matcher.matches()) {
            envMap.put(matcher.group(1), matcher.group(2));
          } else {
            System.err.format("Line \"%s\" is not recognized as 'name=value'%n", line);
          }
        }
      } else {
        if (begin.equals(line)) {
          foundBegin = true;
        }
      }
    }

    if (foundBegin) {
      if (!foundEnd) {
        System.err.format("Problem extracting map for %s -- end marker not found", begin);
      }
    } else {
      System.err.format("Problem extracting map for %s -- markers not found", begin);
    }

    return envMap;
  }
}
