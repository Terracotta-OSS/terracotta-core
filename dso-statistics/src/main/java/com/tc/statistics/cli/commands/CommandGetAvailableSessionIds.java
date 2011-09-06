/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli.commands;

import com.tc.statistics.cli.GathererConnection;

public class CommandGetAvailableSessionIds extends AbstractCliCommand {
  public String[] getArgumentNames() {
    return NO_ARGUMENTS;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    String[] sessionids = connection.getGatherer().getAvailableSessionIds();
    if (null == sessionids ||
        0 == sessionids.length) {
      System.out.println("> Couldn't find any available sessions");
    } else {
      for (int i = 0; i < sessionids.length; i++) {
        System.out.println(sessionids[i]);
      }
    }
  }
}