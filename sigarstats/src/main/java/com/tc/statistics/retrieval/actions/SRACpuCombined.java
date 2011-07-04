/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.statistics.retrieval.SigarUtil;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class SRACpuCombined implements StatisticRetrievalAction, SRACpuConstants {

  public final static TCLogger LOGGER         = TCLogging.getLogger(StatisticRetrievalAction.class);

  public final static String   ACTION_NAME    = "cpu combined";

  private final static String  ELEMENT_PREFIX = "cpu ";

  private final Sigar          sigar;

  private static final boolean SIGAR_ENABLED  = TCPropertiesImpl.getProperties()
                                                  .getBoolean(TCPropertiesConsts.SIGAR_ENABLED);

  public SRACpuCombined() {
    sigar = SIGAR_ENABLED ? SigarUtil.newSigar() : null;
  }

  public StatisticData[] retrieveStatisticData() {
    if (sigar == null) { return EMPTY_STATISTIC_DATA; }
    try {
      NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
      format.setGroupingUsed(false);
      format.setMaximumFractionDigits(3);

      CpuPerc[] cpuPercList = sigar.getCpuPercList();
      StatisticData[] data = new StatisticData[cpuPercList.length];
      for (int i = 0; i < cpuPercList.length; i++) {
        String element = ELEMENT_PREFIX + i;
        double combined = cpuPercList[i].getCombined();
        data[i] = new StatisticData(ACTION_NAME, element, Double.isNaN(combined) || Double.isInfinite(combined) ? null
            : new BigDecimal(format.format(combined)));
      }
      return data;
    } catch (SigarException e) {
      LOGGER.warn("Couldn't retrieve data for statistic '" + ACTION_NAME + "'", e);
      return EMPTY_STATISTIC_DATA;
    }
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }
}
