/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import sun.management.ManagementFactory;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.properties.TCPropertiesImpl;
import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.TCRuntime;
import com.tc.server.TCServer;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.util.ProductInfo;
import com.tc.util.State;
import com.tc.util.StringUtil;
import com.tc.util.runtime.ThreadDumpUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;

public class TCServerInfo extends AbstractTerracottaMBean implements TCServerInfoMBean, StateChangeListener {
  private static final TCLogger                logger          = TCLogging.getLogger(TCServerInfo.class);

  private static final boolean                 DEBUG           = false;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
  static {
    final String[] notifTypes = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
    final String name = AttributeChangeNotification.class.getName();
    final String description = "An attribute of this MBean has changed";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final TCServer                       server;
  private final ProductInfo                    productInfo;
  private final String                         buildID;
  private final L2State                        l2State;

  private final StateChangeNotificationInfo    stateChangeNotificationInfo;
  private long                                 nextSequenceNumber;

  private final JVMMemoryManager               manager;
  private StatisticRetrievalAction             cpuUsageSRA;
  private StatisticRetrievalAction             cpuLoadSRA;
  private String[]                             cpuNames;

  private static final String[]                EMPTY_CPU_NAMES = {};

  private final ObjectStatsRecorder            objectStatsRecorder;

  public TCServerInfo(final TCServer server, final L2State l2State, final ObjectStatsRecorder objectStatsRecorder)
      throws NotCompliantMBeanException {
    super(TCServerInfoMBean.class, true);
    this.server = server;
    this.l2State = l2State;
    this.l2State.registerStateChangeListener(this);
    productInfo = ProductInfo.getInstance();
    buildID = productInfo.buildID();
    nextSequenceNumber = 1;
    stateChangeNotificationInfo = new StateChangeNotificationInfo();
    manager = TCRuntime.getJVMMemoryManager();

    try {
      Class sraCpuLoadType = Class.forName("com.tc.statistics.retrieval.actions.SRACpuLoad");
      if (sraCpuLoadType != null) {
        cpuLoadSRA = (StatisticRetrievalAction) sraCpuLoadType.newInstance();
      }
      Class sraCpuUsageType = Class.forName("com.tc.statistics.retrieval.actions.SRACpuCombined");
      if (sraCpuUsageType != null) {
        cpuUsageSRA = (StatisticRetrievalAction) sraCpuUsageType.newInstance();
      }
    } catch (LinkageError e) {
      /**
       * it's ok not output any errors or warnings here since when the CVT is initialized, it will notify about the
       * incapacity of leading Sigar-based SRAs.
       */
    } catch (ClassNotFoundException e) {
      // Ignored just like linkage error.
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    this.objectStatsRecorder = objectStatsRecorder;
  }

  public ObjectStatsRecorder getObjectStatsRecorder() {
    return this.objectStatsRecorder;
  }

  public void reset() {
    // nothing to reset
  }

  public boolean isStarted() {
    return l2State.isStartState();
  }

  public boolean isActive() {
    return l2State.isActiveCoordinator();
  }

  public boolean isPassiveUninitialized() {
    return l2State.isPassiveUninitialized();
  }

  public boolean isPassiveStandby() {
    return l2State.isPassiveStandby();
  }

  public long getStartTime() {
    return server.getStartTime();
  }

  public long getActivateTime() {
    return server.getActivateTime();
  }

  public boolean isGarbageCollectionEnabled() {
    return server.isGarbageCollectionEnabled();
  }

  public int getGarbageCollectionInterval() {
    return server.getGarbageCollectionInterval();
  }

  public void stop() {
    server.stop();
    _sendNotification("TCServer stopped", "Started", "java.lang.Boolean", Boolean.TRUE, Boolean.FALSE);
  }

  public boolean isShutdownable() {
    return server.canShutdown();
  }

  /**
   * This schedules the shutdown to occur one second after we return from this call because otherwise JMX will be
   * shutdown and we'll get all sorts of other errors trying to return from this call.
   */
  public void shutdown() {
    if (!server.canShutdown()) {
      String msg = "Server cannot be shutdown because it is not fully started.";
      logger.error(msg);
      throw new RuntimeException(msg);
    }
    logger.warn("shutdown is invoked by MBean");
    final Timer timer = new Timer("TCServerInfo shutdown timer");
    final TimerTask task = new TimerTask() {
      @Override
      public void run() {
        server.shutdown();
      }
    };
    timer.schedule(task, 1000);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return Arrays.asList(NOTIFICATION_INFO).toArray(EMPTY_NOTIFICATION_INFO);
  }

  public void startBeanShell(int port) {
    server.startBeanShell(port);
  }

  @Override
  public String toString() {
    if (isStarted()) {
      return "starting, startTime(" + getStartTime() + ")";
    } else if (isActive()) {
      return "active, activateTime(" + getActivateTime() + ")";
    } else {
      return "stopped";
    }
  }

  public String getState() {
    return l2State.toString();
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
    if (productInfo.isPatched()) {
      return productInfo.patchLevel();
    } else {
      return "";
    }
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

  public String getDescriptionOfCapabilities() {
    return server.getDescriptionOfCapabilities();
  }

  public L2Info[] getL2Info() {
    return server.infoForAllL2s();
  }

  public ServerGroupInfo[] getServerGroupInfo() {
    return server.serverGroups();
  }

  public int getDSOListenPort() {
    return server.getDSOListenPort();
  }

  public int getDSOGroupPort() {
    return server.getDSOGroupPort();
  }

  public String[] getCpuStatNames() {
    if (cpuNames != null) return Arrays.asList(cpuNames).toArray(EMPTY_CPU_NAMES);
    if (cpuUsageSRA == null) return cpuNames = EMPTY_CPU_NAMES;

    List list = new ArrayList();
    StatisticData[] statsData = cpuUsageSRA.retrieveStatisticData();
    if (statsData != null) {
      for (StatisticData element : statsData) {
        list.add(element.getElement());
      }
    }
    return cpuNames = (String[]) list.toArray(EMPTY_CPU_NAMES);
  }

  public long getUsedMemory() {
    return manager.getMemoryUsage().getUsedMemory();
  }

  public long getMaxMemory() {
    return manager.getMemoryUsage().getMaxMemory();
  }

  public Map getStatistics() {
    HashMap<String, Object> map = new HashMap<String, Object>();

    map.put(MEMORY_USED, Long.valueOf(getUsedMemory()));
    map.put(MEMORY_MAX, Long.valueOf(getMaxMemory()));

    if (cpuUsageSRA != null) {
      StatisticData[] statsData = getCpuUsage();
      if (statsData != null) {
        map.put(CPU_USAGE, statsData);
      }
    }

    if (cpuLoadSRA != null) {
      StatisticData statsData = getCpuLoad();
      if (statsData != null) {
        map.put(CPU_LOAD, statsData);
      }
    }

    return map;
  }

  private long             lastCpuUsageUpdateTime   = System.currentTimeMillis();
  private StatisticData[]  lastCpuUsageUpdate;
  private long             lastCpuLoadUpdateTime    = System.currentTimeMillis();
  private StatisticData    lastCpuLoadUpdate;
  private static final int CPU_UPDATE_WINDOW_MILLIS = 1000;

  public StatisticData[] getCpuUsage() {
    if (cpuUsageSRA == null) { return null; }
    if (System.currentTimeMillis() - lastCpuUsageUpdateTime < CPU_UPDATE_WINDOW_MILLIS) { return lastCpuUsageUpdate; }
    lastCpuUsageUpdateTime = System.currentTimeMillis();
    return lastCpuUsageUpdate = cpuUsageSRA.retrieveStatisticData();
  }

  public StatisticData getCpuLoad() {
    if (cpuLoadSRA == null) { return null; }
    if (System.currentTimeMillis() - lastCpuLoadUpdateTime < CPU_UPDATE_WINDOW_MILLIS) { return lastCpuLoadUpdate; }
    lastCpuLoadUpdateTime = System.currentTimeMillis();
    StatisticData[] sd = cpuLoadSRA.retrieveStatisticData();
    if (sd.length == 1) { return lastCpuLoadUpdate = sd[0]; }
    return null;
  }

  public byte[] takeCompressedThreadDump(long requestMillis) {
    return ThreadDumpUtil.getCompressedThreadDump();
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
    String[] args = server.processArguments();
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

  public String getPersistenceMode() {
    return server.getPersistenceMode();
  }

  public String getFailoverMode() {
    return server.getFailoverMode();
  }

  public String getConfig() {
    return server.getConfig();
  }

  public String getHealthStatus() {
    // FIXME: the returned value should eventually contain a true representative status of L2 server.
    // for now just return 'OK' to indicate that the process is up-and-running..
    return "OK";
  }

  public void l2StateChanged(StateChangedEvent sce) {
    State state = sce.getCurrentState();

    if (state.equals(StateManager.ACTIVE_COORDINATOR)) {
      server.updateActivateTime();
    }

    debugPrintln("*****  msg=[" + stateChangeNotificationInfo.getMsg(state) + "] attrName=["
                 + stateChangeNotificationInfo.getAttributeName(state) + "] attrType=["
                 + stateChangeNotificationInfo.getAttributeType(state) + "] stateName=[" + state.getName() + "]");

    _sendNotification(stateChangeNotificationInfo.getMsg(state), stateChangeNotificationInfo.getAttributeName(state),
                      stateChangeNotificationInfo.getAttributeType(state), Boolean.FALSE, Boolean.TRUE);
  }

  private synchronized void _sendNotification(String msg, String attr, String type, Object oldVal, Object newVal) {
    sendNotification(new AttributeChangeNotification(this, nextSequenceNumber++, System.currentTimeMillis(), msg, attr,
                                                     type, oldVal, newVal));
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  public boolean getFaultDebug() {
    return objectStatsRecorder.getFaultDebug();
  }

  public void setFaultDebug(boolean faultDebug) {
    objectStatsRecorder.setFaultDebug(faultDebug);
  }

  public boolean getRequestDebug() {
    return objectStatsRecorder.getRequestDebug();
  }

  public void setRequestDebug(boolean requestDebug) {
    objectStatsRecorder.setRequestDebug(requestDebug);
  }

  public boolean getFlushDebug() {
    return objectStatsRecorder.getFlushDebug();
  }

  public void setFlushDebug(boolean flushDebug) {
    objectStatsRecorder.setFlushDebug(flushDebug);
  }

  public boolean getBroadcastDebug() {
    return objectStatsRecorder.getBroadcastDebug();
  }

  public void setBroadcastDebug(boolean broadcastDebug) {
    objectStatsRecorder.setBroadcastDebug(broadcastDebug);
  }

  public boolean getCommitDebug() {
    return objectStatsRecorder.getCommitDebug();
  }

  public void setCommitDebug(boolean commitDebug) {
    objectStatsRecorder.setCommitDebug(commitDebug);
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

  public boolean isEnterprise() {
    return server.getClass().getSimpleName().equals("EnterpriseServerImpl");
  }
}
