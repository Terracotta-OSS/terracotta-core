/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;

/**
 * This statistic gives the fault rate of objects faulted from disk to L2. <p/> The {@link StatisticData} contains the
 * following elements:
 * <ul>
 * <li>fault count</li>
 * </ul>
 * <p/> The {@link com.tc.statistics.retrieval.StatisticsRetriever} samples this data at the global frequency. The
 * property {@code l2.objectmanager.fault.logging.enabled} needs to be {@code true} to collect this statistic.
 */
public class SRAL2FaultsFromDisk implements StatisticRetrievalAction {

  public final static String    ACTION_NAME              = "l2 faults from disk";
  private static final String   ELEMENT_NAME_FAULT_COUNT = "fault count";
  private static final boolean  LOG_ANABLED              = TCPropertiesImpl
                                                             .getProperties()
                                                             .getBoolean(
                                                                         TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED,
                                                                         false);

  private static final TCLogger logger                   = TCLogging.getLogger(SRAL2FaultsFromDisk.class);
  private final SampledCounter  faultCounter;

  public SRAL2FaultsFromDisk(final DSOGlobalServerStats serverStats) {
    if (!LOG_ANABLED) {
      this.faultCounter = null;
      logger.info("\"" + ACTION_NAME + "\" statistic is not enabled. Please enable the property \""
                  + TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED + "\"" + " to collect this statistic.");
    } else {
      faultCounter = serverStats.getL2FaultFromDiskCounter();
    }
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    if (faultCounter == null) return EMPTY_STATISTIC_DATA;
    TimeStampedCounterValue value = faultCounter.getMostRecentSample();
    return new StatisticData[] { new StatisticData(ACTION_NAME, ELEMENT_NAME_FAULT_COUNT, value.getCounterValue()) };
  }
}