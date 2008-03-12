/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.l1;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCRuntime;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.util.runtime.ThreadDumpUtil;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.management.NotCompliantMBeanException;

public class L1Info extends AbstractTerracottaMBean implements L1InfoMBean {
  private static final TCLogger    logger = TCLogging.getLogger(L1Info.class);
  private final String             rawConfigText;
  private final JVMMemoryManager   manager;
  private StatisticRetrievalAction cpuSRA;

  public L1Info(String rawConfigText) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, false);

    this.manager = TCRuntime.getJVMMemoryManager();
    this.rawConfigText = rawConfigText;
    try {
      Class sraCpuType = Class.forName("com.tc.statistics.retrieval.actions.SRACpu");
      if (sraCpuType != null) {
        this.cpuSRA = (StatisticRetrievalAction) sraCpuType.newInstance();
      }
    } catch (Exception e) {
      /**/
    }
  }

  public String getEnvironment() {
    StringBuffer sb = new StringBuffer();
    Properties env = System.getProperties();
    Enumeration keys = env.propertyNames();
    ArrayList l = new ArrayList();

    while (keys.hasMoreElements()) {
      Object o = keys.nextElement();
      if (o instanceof String) {
        String key = (String) o;
        l.add(key);
      }
    }

    int maxKeyLen = 0;
    Iterator iter = l.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      maxKeyLen = Math.max(key.length(), maxKeyLen);
    }

    iter = l.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      sb.append(key);
      sb.append(":");
      int spaceLen = maxKeyLen - key.length() + 1;
      for (int i = 0; i < spaceLen; i++) {
        sb.append(" ");
      }
      sb.append(env.getProperty(key));
      sb.append("\n");
    }

    return sb.toString();
  }

  public String getConfig() {
    return rawConfigText;
  }

  public String takeThreadDump(long requestMillis) {
    String text = ThreadDumpUtil.getThreadDump();
    logger.info(text);
    return text;
  }

  public Map getStatistics() {
    HashMap map = new HashMap();
    MemoryUsage usage = manager.getMemoryUsage();

    map.put("memory used", new Long(usage.getUsedMemory()));
    map.put("memory max", new Long(usage.getMaxMemory()));

    if (cpuSRA != null) {
      StatisticData[] statsData = cpuSRA.retrieveStatisticData();
      if (statsData != null) {
        map.put("cpu usage", statsData);
      }
    }

    return map;
  }

  public void reset() {
    /**/
  }

}
