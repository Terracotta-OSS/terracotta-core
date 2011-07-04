/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.util.Date;

/**
 * Statistic representing the shutdown of a statistics capturing session.
 */
public class SRAShutdownTimestamp implements StatisticRetrievalAction {

  public final static String ACTION_NAME = "shutdown timestamp";

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return null;
  }

  public StatisticData[] retrieveStatisticData() {
    Date moment = new Date();
    return new StatisticData[] { new StatisticData(ACTION_NAME, moment) };
  }
}