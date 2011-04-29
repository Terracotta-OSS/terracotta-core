/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

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

public class SRACpuLoad implements StatisticRetrievalAction, SRACpuConstants {
  public final static TCLogger LOGGER        = TCLogging.getLogger(StatisticRetrievalAction.class);

  public final static String   ACTION_NAME   = "cpu load";
  private final static String  ELEMENT       = "cpu";

  private final Sigar          sigar;

  private static final boolean SIGAR_ENABLED = TCPropertiesImpl.getProperties()
                                                 .getBoolean(TCPropertiesConsts.SIGAR_ENABLED);

  public SRACpuLoad() {
    sigar = SIGAR_ENABLED ? SigarUtil.newSigar() : null;
  }

  public StatisticData[] retrieveStatisticData() {
    if (sigar == null) { return EMPTY_STATISTIC_DATA; }

    NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
    format.setGroupingUsed(false);
    format.setMaximumFractionDigits(3);

    try {
      double combined = sigar.getCpuPerc().getCombined();
      return new StatisticData[] { new StatisticData(ACTION_NAME, ELEMENT, Double.isNaN(combined)
                                                                           || Double.isInfinite(combined) ? null
          : new BigDecimal(format.format(combined))) };
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
