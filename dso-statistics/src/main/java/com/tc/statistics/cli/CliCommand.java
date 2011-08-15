/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli;

public interface CliCommand {
  public final static String[] NO_ARGUMENTS = new String[0];

  public String getCommandName();
  public String[] getArgumentNames();
  public void execute(GathererConnection connection, String[] arguments);
}
