/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.cli.commands;

import com.tc.statistics.cli.GathererConnection;
import com.tc.util.Assert;

public class CommandGetGlobalParam extends AbstractCliCommand {
  private final static String[] ARGUMENT_NAMES = new String[] { "key" };

  public String[] getArgumentNames() {
    return ARGUMENT_NAMES;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    Assert.assertEquals(ARGUMENT_NAMES.length, arguments.length);
    Object value = connection.getGatherer().getGlobalParam(arguments[0]);
    if (null == value) {
      System.out.println("> Global parameter '" + arguments[0] + "' isn't set.");
    } else {
      System.out.println(value);
    }
  }
}