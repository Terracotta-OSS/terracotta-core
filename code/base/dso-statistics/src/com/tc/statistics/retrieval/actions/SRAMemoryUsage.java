/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCRuntime;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.text.DecimalFormat;

/**
 * This statistics gives memory usage statistics.
 * <p/>
 * Contains {@link StatisticData} with elements for the amount of free memory, used memory and max memory
 */
public class SRAMemoryUsage implements StatisticRetrievalAction {

  public static final double     KB             = 1024D;
  public static final double     MB             = KB * KB;

  public final static String     ACTION_NAME    = "memory";

  public final static String     DATA_NAME_FREE = ACTION_NAME + " free";
  public final static String     DATA_NAME_USED = ACTION_NAME + " used";
  public final static String     DATA_NAME_MAX  = ACTION_NAME + " max";

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
    return new StatisticData[] { new StatisticData(DATA_NAME_FREE, formatMemory(usage.getFreeMemory())),
        new StatisticData(DATA_NAME_USED, formatMemory(usage.getUsedMemory())),
        new StatisticData(DATA_NAME_MAX, formatMemory(usage.getMaxMemory())) };
  }

  private String formatMemory(long memory) {
    if (memory >= MB) {
      DecimalFormat decimalFormat = new DecimalFormat("#.000000 MB");
      return decimalFormat.format(memory / MB);
    } else if (memory >= KB) {
      DecimalFormat decimalFormat = new DecimalFormat("#.000 KB");
      return decimalFormat.format(memory / KB);
    } else {
      return memory + " Bytes";
    }
  }
}