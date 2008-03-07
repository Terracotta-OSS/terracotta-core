/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli.commands;

import com.tc.statistics.cli.GathererConnection;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererAlreadyConnectedException;

import javax.management.RuntimeMBeanException;

public class CommandConnect extends AbstractCliCommand {

  public String[] getArgumentNames() {
    return NO_ARGUMENTS;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    try {
      connection.getGatherer().connect();
      System.out.println("> Connected.");
    } catch (RuntimeMBeanException e) {
      Throwable cause1 = e.getCause();
      if (cause1 != null &&
          cause1 instanceof RuntimeException) {
        Throwable cause2 = cause1.getCause();
        if (cause2 != null &&
            cause2 instanceof TCStatisticsGathererAlreadyConnectedException) {
          System.out.println("> Already connected.");
          return;
        }
      }
      throw e;
    }
  }
}
