/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCRuntime;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

/**
 * This statistics gives memory usage statistics.
 * Contains {link StatisticData} with elements for the amount of free memory, used memory
 * and max memory
 */
public class SRAMemoryUsage implements StatisticRetrievalAction {
  
  public final static String ACTION_NAME = "memory";

  public final static String DATA_NAME_FREE = ACTION_NAME + " free";
  public final static String DATA_NAME_USED = ACTION_NAME + " used";
  public final static String DATA_NAME_MAX = ACTION_NAME + " max";

  private final JVMMemoryManager manager;

  public SRAMemoryUsage() {
    manager = TCRuntime.getJVMMemoryManager();
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    MemoryUsage usage = manager.getMemoryUsage();
    return new StatisticData[] {
      new StatisticData(DATA_NAME_FREE, new Long(usage.getFreeMemory())),
      new StatisticData(DATA_NAME_USED, new Long(usage.getUsedMemory())),
      new StatisticData(DATA_NAME_MAX, new Long(usage.getMaxMemory()))
    };
  }
}