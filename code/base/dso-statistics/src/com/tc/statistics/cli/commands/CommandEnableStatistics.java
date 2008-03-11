/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.cli.commands;

import org.apache.commons.lang.StringUtils;

import com.tc.statistics.cli.GathererConnection;
import com.tc.util.Assert;

public class CommandEnableStatistics extends AbstractCliCommand {
  public final static String[] ARGUMENT_NAMES = new String[] {"comma-separated list of names"};

  public String[] getArgumentNames() {
    return ARGUMENT_NAMES;
  }

  public void execute(final GathererConnection connection, final String[] arguments) {
    Assert.assertEquals(ARGUMENT_NAMES.length, arguments.length);
    String[] statistics = StringUtils.split(arguments[0], ',');
    for (int i = 0; i < statistics.length; i++) {
      statistics[i] = StringUtils.trim(statistics[i]);
    }
    connection.getGatherer().enableStatistics(statistics);
    System.out.println("> Statistics '" + arguments[0] + "' enabled.");
  }
}