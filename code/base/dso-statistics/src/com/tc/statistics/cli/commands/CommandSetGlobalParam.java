/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.cli.commands;

import com.tc.statistics.cli.GathererConnection;
import com.tc.util.Assert;

public class CommandSetGlobalParam extends AbstractCliCommand {
  private final static String[] ARGUMENT_NAMES = new String[] { "key", "value" };

  public String[] getArgumentNames() {
    return ARGUMENT_NAMES;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    Assert.assertEquals(ARGUMENT_NAMES.length, arguments.length);
    connection.getGatherer().setGlobalParam(arguments[0], arguments[1]);
    System.out.println("> Global parameter '" + arguments[0] + "' set to '" + arguments[1] + "'.");
  }
}