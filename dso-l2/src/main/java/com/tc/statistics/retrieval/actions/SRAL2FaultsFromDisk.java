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

  public static final String    ACTION_NAME                             = "l2 faults from disk";
  public static final String    ELEMENT_NAME_FAULT_COUNT                = "fault count";
  public static final String    ELEMENT_NAME_AVG_TIME_2_FAULT_FROM_DISK = "avg time to fault from disk";
  public static final String    ELEMENT_NAME_AVG_TIME_2_ADD_2_OBJ_MGR   = "avg time to add to Object Manager";

  private static final boolean  LOG_ANABLED                             = TCPropertiesImpl
                                                                            .getProperties()
                                                                            .getBoolean(
                                                                                        TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED,
                                                                                        false);

  private static final TCLogger logger                                  = TCLogging
                                                                            .getLogger(SRAL2FaultsFromDisk.class);
  private final SampledCounter  faultCounter;
  private final SampledCounter  time2FaultFromDisk;
  private final SampledCounter  time2Add2ObjectMgr;

  public SRAL2FaultsFromDisk(final DSOGlobalServerStats serverStats) {
    if (!LOG_ANABLED) {
      this.faultCounter = null;
      this.time2FaultFromDisk = null;
      this.time2Add2ObjectMgr = null;
      logger.info("\"" + ACTION_NAME + "\" statistic is not enabled. Please enable the property \""
                  + TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED + "\"" + " to collect this statistic.");
    } else {
      faultCounter = serverStats.getL2FaultFromDiskCounter();
      time2FaultFromDisk = serverStats.getTime2FaultFromDisk();
      time2Add2ObjectMgr = serverStats.getTime2Add2ObjectMgr();
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
    TimeStampedCounterValue sample = faultCounter.getMostRecentSample();
    long faultCount = sample == null ? 0 : sample.getCounterValue();

    sample = time2FaultFromDisk.getMostRecentSample();
    long time2Fault = sample == null ? 0 : sample.getCounterValue();

    sample = time2Add2ObjectMgr.getMostRecentSample();
    long time2Add = sample == null ? 0 : sample.getCounterValue();

    return new StatisticData[] {
        new StatisticData(ACTION_NAME, ELEMENT_NAME_FAULT_COUNT, faultCount),
        new StatisticData(ACTION_NAME, ELEMENT_NAME_AVG_TIME_2_FAULT_FROM_DISK, (faultCount == 0 ? 0 : time2Fault
                                                                                                       / faultCount)),
        new StatisticData(ACTION_NAME, ELEMENT_NAME_AVG_TIME_2_ADD_2_OBJ_MGR, (faultCount == 0 ? 0 : time2Add
                                                                                                     / faultCount)) };

  }
}