/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.util;

import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class used for interpolation of properties.
 */
public class PropertiesInterpolator {
  public static final Pattern variablePattern = Pattern.compile("\\$\\{(.*?)\\}");

  /**
   * Returns a new Properties object containing the same set of properties as
   * the given <code>properties</code>.  Property values may contain variables,
   * which are of the form ${variable}.  Any property value that contains one
   * or more occurrences of a variable will have its variables interpolated
   * against the given properties object, system properties, and environment
   * variables, in that order.  If a variable is not found through this search
   * process, it is left as-is in the interpolated properties.
   */
  public Properties interpolated(Properties properties) {
    Properties result = new Properties();
    for (Enumeration<String> names = (Enumeration<String>) properties.propertyNames();
         names.hasMoreElements(); ) {
      String name = names.nextElement();
      String value = properties.getProperty(name);
      result.setProperty(name, interpolate(value, properties));
    }
    return result;
  }

  private String interpolate(String value, Properties props) {
    StringBuffer buf = new StringBuffer();
    Matcher matcher = variablePattern.matcher(value);
    int startIndex = 0;
    while (matcher.find()) {
      buf.append(value.substring(startIndex, matcher.start()));
      String resolved = resolveVariable(matcher.group(1), props);
      if (resolved == null) {
        resolved = matcher.group();
      }
      buf.append(resolved);
      startIndex = matcher.end();
    }
    buf.append(value.substring(startIndex));

    return buf.toString();
  }
  
  private String resolveVariable(String variable, Properties props) {
    String value = props.getProperty(variable);
    if (value == null) {
      value = System.getProperty(variable);
    }
    if (value == null) {
      value = System.getenv(variable);
    }
    return value;
  }
}
