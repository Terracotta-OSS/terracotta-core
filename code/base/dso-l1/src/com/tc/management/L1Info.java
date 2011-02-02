/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.handler.LockInfoDumpHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.TCRuntime;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.util.ProductInfo;
import com.tc.util.StringUtil;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.LockInfoByThreadIDImpl;
import com.tc.util.runtime.ThreadDumpUtil;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;

public class L1Info extends AbstractTerracottaMBean implements L1InfoMBean {
  private static final TCLogger                logger = TCLogging.getLogger(L1Info.class);

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
  static {
    final String[] notifTypes = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
    final String name = AttributeChangeNotification.class.getName();
    final String description = "An attribute of this MBean has changed";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }
  private long                                 nextSequenceNumber;

  private final ProductInfo                    productInfo;
  private final String                         buildID;
  private final String                         rawConfigText;
  private final JVMMemoryManager               manager;
  private StatisticRetrievalAction             cpuSRA;
  private String[]                             cpuNames;
  private final TCClient                       client;
  private final LockInfoDumpHandler            lockInfoDumpHandler;

  public L1Info(TCClient client, String rawConfigText) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, true);

    this.productInfo = ProductInfo.getInstance();
    this.buildID = productInfo.buildID();
    this.manager = TCRuntime.getJVMMemoryManager();
    this.rawConfigText = rawConfigText;
    this.client = client;
    this.lockInfoDumpHandler = client;
    this.nextSequenceNumber = 1;
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

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return Arrays.asList(NOTIFICATION_INFO).toArray(EMPTY_NOTIFICATION_INFO);
  }

  private synchronized void _sendNotification(String msg, String attr, String type, Object oldVal, Object newVal) {
    sendNotification(new AttributeChangeNotification(this, nextSequenceNumber++, System.currentTimeMillis(), msg, attr,
                                                     type, oldVal, newVal));
  }

  // for tests
  public L1Info(LockInfoDumpHandler lockInfoDumpHandler) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, true);
    this.productInfo = ProductInfo.getInstance();
    this.buildID = productInfo.buildID();
    this.rawConfigText = null;
    this.manager = TCRuntime.getJVMMemoryManager();
    this.lockInfoDumpHandler = lockInfoDumpHandler;
    this.client = null;
    this.nextSequenceNumber = 1;
  }

  public String getVersion() {
    return productInfo.toShortString();
  }

  public String getMavenArtifactsVersion() {
    return productInfo.mavenArtifactsVersion();
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
    return format(System.getProperties());
  }

  public String getTCProperties() {
    Properties props = TCPropertiesImpl.getProperties().addAllPropertiesTo(new Properties());
    String keyPrefix = /* TCPropertiesImpl.SYSTEM_PROP_PREFIX */null;
    return format(props, keyPrefix);
  }

  private String format(Properties properties) {
    return format(properties, null);
  }

  private String format(Properties properties, String keyPrefix) {
    StringBuffer sb = new StringBuffer();
    Enumeration keys = properties.propertyNames();
    ArrayList<String> l = new ArrayList<String>();

    while (keys.hasMoreElements()) {
      Object o = keys.nextElement();
      if (o instanceof String) {
        String key = (String) o;
        l.add(key);
      }
    }

    String[] props = l.toArray(new String[l.size()]);
    Arrays.sort(props);
    l.clear();
    l.addAll(Arrays.asList(props));

    int maxKeyLen = 0;
    for (String key : l) {
      maxKeyLen = Math.max(key.length(), maxKeyLen);
    }

    for (String key : l) {
      if (keyPrefix != null) {
        sb.append(keyPrefix);
      }
      sb.append(key);
      sb.append(":");
      int spaceLen = maxKeyLen - key.length() + 1;
      for (int i = 0; i < spaceLen; i++) {
        sb.append(" ");
      }
      sb.append(properties.getProperty(key));
      sb.append("\n");
    }

    return sb.toString();
  }

  public String[] getProcessArguments() {
    String[] args = client.processArguments();
    List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
    if (args == null) {
      return inputArgs.toArray(new String[inputArgs.size()]);
    } else {
      List<String> l = new ArrayList<String>();
      l.add(StringUtil.toString(args, " ", null, null));
      l.addAll(inputArgs);
      return l.toArray(new String[l.size()]);
    }
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

  public byte[] takeCompressedThreadDump(long requestMillis) {
    LockInfoByThreadID lockInfo = new LockInfoByThreadIDImpl();
    this.lockInfoDumpHandler.addAllLocksTo(lockInfo);
    return ThreadDumpUtil.getCompressedThreadDump(lockInfo, this.lockInfoDumpHandler.getThreadIDMap());
  }

  public String[] getCpuStatNames() {
    if (cpuNames != null) return cpuNames;
    if (cpuSRA == null) return cpuNames = new String[0];

    List list = new ArrayList();
    StatisticData[] statsData = cpuSRA.retrieveStatisticData();
    if (statsData != null) {
      for (StatisticData element : statsData) {
        list.add(element.getElement());
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

  public void gc() {
    ManagementFactory.getMemoryMXBean().gc();
  }

  public boolean isVerboseGC() {
    return ManagementFactory.getMemoryMXBean().isVerbose();
  }

  public void setVerboseGC(boolean verboseGC) {
    boolean oldValue = isVerboseGC();
    ManagementFactory.getMemoryMXBean().setVerbose(verboseGC);
    _sendNotification("VerboseGC changed", "VerboseGC", "java.lang.Boolean", oldValue, verboseGC);
  }
}
