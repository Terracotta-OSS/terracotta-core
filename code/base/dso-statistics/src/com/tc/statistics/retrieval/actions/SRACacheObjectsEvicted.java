/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.object.cache.CacheManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

/**
 * This statistics action represents the statistics regarding the objects evicted from the
 * {@link CacheManager}. The statistics contains {@link StatisticData} with the following elements
 * <ul>
 * <li>evicted count</li>
 * <li>current count</li>
 * <li>new objects count</li>
 * <li>time taken</li>
 * </ul>
 * This statistic action should not be used to retrieve the cache manager objects evicted stats.
 * The actual collection of this statistic is done in the {@link CacheManager} and injected into
 * the statistics sub-system.
 */
public class SRACacheObjectsEvicted implements StatisticRetrievalAction {

  public static final String ACTION_NAME = CacheManager.CACHE_OBJECTS_EVICTED;

  public StatisticData[] retrieveStatisticData() {
    throw new UnsupportedOperationException(ACTION_NAME + " Statistics cannot be retrieved using the action instance." +
                                            " It will be collected automatically when triggered by the system.");
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.TRIGGERED;
  }
}