/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for calculating and verifying the MD5 sum of a file.
 */
public class CommandUtil {
  private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("([A-Za-z0-9_]+)Command");

  /**
   * Returns the name of the class (in lowercase) minus the "Command" suffix if it has one.
   * 
   * @param klass the class from which the command name should be deducted
   * @return the deducted command name
   */
  public static String deductNameFromClass(final Class klass) {
    String commandName = klass.getSimpleName();
    Matcher matcher = CLASS_NAME_PATTERN.matcher(commandName);
    if (matcher.matches()) {
      commandName = matcher.group(1);
    }
    return commandName.toLowerCase();
  }
}