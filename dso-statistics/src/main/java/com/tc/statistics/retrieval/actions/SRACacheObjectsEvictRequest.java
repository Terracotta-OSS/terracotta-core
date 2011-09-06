/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.cache.CacheManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

/**
 * This statistics action represents the statistics regarding the request of evicting objects from the
 * {@link CacheManager}.
 * <p/>
 * The statistics contains {@link StatisticData} with the following elements
 * <ul>
 * <li>asking to evict count</li>
 * <li>current size</li>
 * <li>calculated cache size</li>
 * <li>percentage heap used</li>
 * <li>gc count</li>
 * </ul>
 * <p/>
 * This statistic action should not be used to retrieve the cache manager objects evict request stats.
 * The actual collection of this statistic is done in the {@link CacheManager} and injected into
 * the statistics sub-system.
 */
public class SRACacheObjectsEvictRequest implements StatisticRetrievalAction {

  public static final String   ACTION_NAME = CacheManager.CACHE_OBJECTS_EVICT_REQUEST;

  public final static TCLogger LOGGER      = TCLogging.getLogger(StatisticRetrievalAction.class);

  public StatisticData[] retrieveStatisticData() {
    LOGGER.warn("Data for statistic '" + ACTION_NAME + " can't be retrieved using the action instance. "
                + "It will be collected automatically when triggered by the system.");
    return EMPTY_STATISTIC_DATA;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.TRIGGERED;
  }
}
