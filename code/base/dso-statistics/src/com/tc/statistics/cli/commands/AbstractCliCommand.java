/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli.commands;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.cli.CliCommand;

public abstract class AbstractCliCommand implements CliCommand {
  public final static String COMMAND_CLASS_PREFIX = "Command";

  public String getCommandName() {
    String short_name = ClassUtils.getShortClassName(getClass());
    if (!short_name.startsWith(COMMAND_CLASS_PREFIX)) {
      throw new TCRuntimeException("The class name " + getClass().getName() + " doesn't start with '" + COMMAND_CLASS_PREFIX + "'.");
    }
    return StringUtils.uncapitalize(short_name.substring(COMMAND_CLASS_PREFIX.length()));
  }
}
