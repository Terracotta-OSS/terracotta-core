/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class SRACpu implements StatisticRetrievalAction, SRACpuConstants {
  
  private final static String ELEMENT_PREFIX = "cpu ";

  private final Sigar sigar;

  public SRACpu() {
    sigar = new Sigar();
  }

  public StatisticData[] retrieveStatisticData() {
    try {
      NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
      format.setGroupingUsed(false);
      format.setMaximumFractionDigits(3);

      CpuPerc[] cpuPercList = sigar.getCpuPercList();
      StatisticData[] data = new StatisticData[cpuPercList.length * 6];
      for (int i = 0; i < cpuPercList.length; i++) {
        String element = ELEMENT_PREFIX + i;
        double combined = cpuPercList[i].getCombined();
        double idle = cpuPercList[i].getIdle();
        double nice = cpuPercList[i].getNice();
        double sys = cpuPercList[i].getSys();
        double user = cpuPercList[i].getUser();
        double wait = cpuPercList[i].getWait();
        data[i * 6] = new StatisticData(DATA_NAME_COMBINED, element, Double.isNaN(combined) || Double.isInfinite(combined) ? null : new BigDecimal(format.format(combined)));
        data[i * 6 + 1] = new StatisticData(DATA_NAME_IDLE, element, Double.isNaN(idle) || Double.isInfinite(idle) ? null : new BigDecimal(format.format(idle)));
        data[i * 6 + 2] = new StatisticData(DATA_NAME_NICE, element, Double.isNaN(nice) || Double.isInfinite(nice) ? null : new BigDecimal(format.format(nice)));
        data[i * 6 + 3] = new StatisticData(DATA_NAME_SYS, element, Double.isNaN(sys) || Double.isInfinite(sys) ? null : new BigDecimal(format.format(sys)));
        data[i * 6 + 4] = new StatisticData(DATA_NAME_USER, element, Double.isNaN(user) || Double.isInfinite(user) ? null : new BigDecimal(format.format(user)));
        data[i * 6 + 5] = new StatisticData(DATA_NAME_WAIT, element, Double.isNaN(wait) || Double.isInfinite(wait) ? null : new BigDecimal(format.format(wait)));
      }
      return data;
    } catch (SigarException e) {
      throw new TCRuntimeException(e);
    }
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}