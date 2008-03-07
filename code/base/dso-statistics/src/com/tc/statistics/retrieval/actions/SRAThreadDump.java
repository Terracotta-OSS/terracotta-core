/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.util.runtime.ThreadDumpUtil;

import java.util.Date;

public class SRAThreadDump implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "thread dump";

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    Date moment = new Date();
    return new StatisticData[] { new StatisticData(ACTION_NAME, moment, ThreadDumpUtil.getThreadDump()) };
  }
}
