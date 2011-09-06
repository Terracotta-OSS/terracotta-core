/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.util.runtime.ThreadDumpUtil;

/**
 * Statistic that gives the thread dump of the system.
 * <p/>
 * This statistic will only run in JRE-1.5 or later. It will not give thread
 * dumps for JRE-1.4 and less.
 */
public class SRAThreadDump implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "thread dump";

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.TRIGGERED;
  }

  public StatisticData[] retrieveStatisticData() {
    return new StatisticData[] { new StatisticData(ACTION_NAME, ThreadDumpUtil.getThreadDump()) };
  }
}
