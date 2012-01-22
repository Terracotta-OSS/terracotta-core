/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.retrieval;

import com.tc.statistics.StatisticRetrievalAction;

import java.util.Collection;
import java.util.Collections;

public class NullStatisticsRetrievalRegistry implements StatisticsRetrievalRegistry {

  public static final NullStatisticsRetrievalRegistry INSTANCE = new NullStatisticsRetrievalRegistry();

  public void removeAllActionInstances() {
    //no-op
  }

  public Collection getSupportedStatistics() {
    return Collections.EMPTY_LIST;
  }

  public Collection getRegisteredActionInstances() {
    return Collections.EMPTY_LIST;
  }

  public StatisticRetrievalAction getActionInstance(String name) {
    return null;
  }

  public void registerActionInstance(StatisticRetrievalAction action) {
    //no-op
  }

  public void registerActionInstance(String sraClassName) {
    //no-op
  }
}
