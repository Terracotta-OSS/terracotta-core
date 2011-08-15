/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.cache.CacheManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

/**
 * This statistics action represents the statistics regarding the objects evicted from the {@link CacheManager}.
 * <p/>
 * The statistics contains {@link StatisticData} with the following elements
 * <ul>
 * <li>evicted count</li>
 * <li>current count</li>
 * <li>new objects count</li>
 * <li>time taken</li>
 * </ul>
 * <p/>
 * In case of L2, this gives the number of objects flushed to the disk by the cache manager while in case of L1 it gives
 * the number of objects flushed from L1 to L2. It should be noted that in case of L1 the objects evicted from the cache
 * manager does not necessarily mean that those objects will be garbage collected from the node as other local objects
 * might be still having reference to it.
 * <p/>
 * This statistic action should not be used to retrieve the cache manager objects evicted stats. The actual collection
 * of this statistic is done in the {@link CacheManager} and injected into the statistics sub-system.
 */
public class SRACacheObjectsEvicted implements StatisticRetrievalAction {

  public final static TCLogger LOGGER      = TCLogging.getLogger(StatisticRetrievalAction.class);

  public static final String   ACTION_NAME = CacheManager.CACHE_OBJECTS_EVICTED;

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