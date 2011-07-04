/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli.commands;

import com.tc.statistics.cli.GathererConnection;

public class CommandGetSupportedStatistics extends AbstractCliCommand {
  public String[] getArgumentNames() {
    return NO_ARGUMENTS;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    String[] stats = connection.getGatherer().getSupportedStatistics();
    if (null == stats ||
        0 == stats.length) {
      System.out.println("> Couldn't find any supported statistics");
    } else {
      for (int i = 0; i < stats.length; i++) {
        System.out.println(stats[i]);
      }
    }
  }
}