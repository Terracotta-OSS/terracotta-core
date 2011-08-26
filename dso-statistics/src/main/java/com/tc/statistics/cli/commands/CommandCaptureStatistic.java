/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.cli.commands;

import com.tc.statistics.StatisticData;
import com.tc.statistics.cli.GathererConnection;
import com.tc.util.Assert;

public class CommandCaptureStatistic extends AbstractCliCommand {
  private final static String[] ARGUMENT_NAMES = new String[] { "name" };

  public String[] getArgumentNames() {
    return ARGUMENT_NAMES;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    Assert.assertEquals(ARGUMENT_NAMES.length, arguments.length);
    StatisticData[] data = connection.getGatherer().captureStatistic(arguments[0]);
    if (null == data || 0 == data.length) {
      System.out.println("> Statistic '" + arguments[0] + "' couldn't be captured.");
    } else {
      for (StatisticData element : data) {
        System.out.print(element.toCsv());
      }
    }
  }
}