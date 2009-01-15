/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.handler.LockInfoDumpHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.TCRuntime;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.util.ProductInfo;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.LockInfoByThreadIDImpl;
import com.tc.util.runtime.ThreadDumpUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.NotCompliantMBeanException;

public class L1Info extends AbstractTerracottaMBean implements L1InfoMBean {
  private static final TCLogger     logger = TCLogging.getLogger(L1Info.class);
  private final ProductInfo         productInfo;
  private final String              buildID;

  private final String              rawConfigText;
  private final JVMMemoryManager    manager;
  private StatisticRetrievalAction  cpuSRA;
  private String[]                  cpuNames;
  private final TCClient            client;
  private final LockInfoDumpHandler lockInfoDumpHandler;

  public L1Info(TCClient client, String rawConfigText) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, false);

    this.productInfo = ProductInfo.getInstance();
    this.buildID = productInfo.buildID();
    this.manager = TCRuntime.getJVMMemoryManager();
    this.rawConfigText = rawConfigText;
    this.client = client;
    this.lockInfoDumpHandler = client;
    try {
      Class sraCpuType = Class.forName("com.tc.statistics.retrieval.actions.SRACpuCombined");
      if (sraCpuType != null) {
        this.cpuSRA = (StatisticRetrievalAction) sraCpuType.newInstance();
      }
    } catch (LinkageError e) {
      /**
       * it's ok not output any errors or warnings here since when the CVT is initialized, it will notify about the
       * incapacity of leading Sigar-based SRAs.
       */
    } catch (Exception e) {
      /**/
    }
  }

  // for tests
  public L1Info(LockInfoDumpHandler lockInfoDumpHandler) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, false);
    this.productInfo = ProductInfo.getInstance();
    this.buildID = productInfo.buildID();
    this.rawConfigText = null;
    this.manager = TCRuntime.getJVMMemoryManager();
    this.lockInfoDumpHandler = lockInfoDumpHandler;
    this.client = null;
  }

  public String getVersion() {
    return productInfo.toShortString();
  }

  public String getBuildID() {
    return buildID;
  }

  public boolean isPatched() {
    return productInfo.isPatched();
  }

  public String getPatchLevel() {
    return productInfo.patchLevel();
  }

  public String getPatchVersion() {
    if (productInfo.isPatched()) {
      return productInfo.toLongPatchString();
    } else {
      return "";
    }
  }

  public String getPatchBuildID() {
    if (productInfo.isPatched()) {
      return productInfo.patchBuildID();
    } else {
      return "";
    }
  }

  public String getCopyright() {
    return productInfo.copyright();
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

    String[] props = (String[]) l.toArray(new String[0]);
    Arrays.sort(props);
    l.clear();
    l.addAll(Arrays.asList(props));

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
    LockInfoByThreadID lockInfo = new LockInfoByThreadIDImpl();
    this.lockInfoDumpHandler.addAllLocksTo(lockInfo);
    String text = ThreadDumpUtil.getThreadDump(lockInfo, this.lockInfoDumpHandler.getThreadIDMap());
    logger.info(text);
    return text;
  }

  public String[] getCpuStatNames() {
    if (cpuNames != null) return cpuNames;
    if (cpuSRA == null) return cpuNames = new String[0];

    List list = new ArrayList();
    StatisticData[] statsData = cpuSRA.retrieveStatisticData();
    if (statsData != null) {
      for (int i = 0; i < statsData.length; i++) {
        list.add(statsData[i].getElement());
      }
    }
    return cpuNames = (String[]) list.toArray(new String[0]);
  }

  public Map getStatistics() {
    HashMap map = new HashMap();

    map.put(MEMORY_USED, new Long(getUsedMemory()));
    map.put(MEMORY_MAX, new Long(getMaxMemory()));

    if (cpuSRA != null) {
      StatisticData[] statsData = getCpuUsage();
      if (statsData != null) {
        map.put(CPU_USAGE, statsData);
      }
    }

    return map;
  }

  public long getUsedMemory() {
    return manager.getMemoryUsage().getUsedMemory();
  }
  
  public long getMaxMemory() {
    return manager.getMemoryUsage().getMaxMemory();
  }

  private long             lastCpuUpdateTime        = System.currentTimeMillis();
  private StatisticData[]  lastCpuUpdate;
  private static final int CPU_UPDATE_WINDOW_MILLIS = 1000;

  public StatisticData[] getCpuUsage() {
    if (cpuSRA == null) return null;
    if (System.currentTimeMillis() - lastCpuUpdateTime < CPU_UPDATE_WINDOW_MILLIS) { return lastCpuUpdate; }
    lastCpuUpdateTime = System.currentTimeMillis();
    return lastCpuUpdate = cpuSRA.retrieveStatisticData();
  }

  public void reset() {
    /**/
  }

  public void startBeanShell(int port) {
    this.client.startBeanShell(port);
  }

}
