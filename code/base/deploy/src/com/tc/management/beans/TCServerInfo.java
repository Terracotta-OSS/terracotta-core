/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.config.schema.L2Info;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCRuntime;
import com.tc.server.TCServer;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.util.ProductInfo;
import com.tc.util.State;
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
  private StatisticRetrievalAction             cpuSRA;
  private String[]                             cpuNames;

  private static final String[]                EMPTY_CPU_NAMES = {};

  public TCServerInfo(final TCServer server, final L2State l2State) throws NotCompliantMBeanException {
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
      Class sraCpuType = Class.forName("com.tc.statistics.retrieval.actions.SRACpuCombined");
      if (sraCpuType != null) {
        cpuSRA = (StatisticRetrievalAction) sraCpuType.newInstance();
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

  public void stop() {
    server.stop();
    _sendNotification("TCServer stopped", "Started", "jmx.terracotta.L2.stopped", Boolean.TRUE, Boolean.FALSE);
  }

  public boolean isShutdownable() {
    return server.canShutdown();
  }

  /**
   * This schedules the shutdown to occur one second after we return from this call because otherwise JMX will be
   * shutdown and we'll get all sorts of other errors trying to return from this call.
   */
  public void shutdown() {
    if (!server.canShutdown()) { throw new RuntimeException(
                                                            "Server cannot be shutdown because it is not fully started."); }
    final Timer timer = new Timer();
    final TimerTask task = new TimerTask() {
      public void run() {
        server.shutdown();
      }
    };
    timer.schedule(task, 1000);
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return Arrays.asList(NOTIFICATION_INFO).toArray(EMPTY_NOTIFICATION_INFO);
  }

  public void startBeanShell(int port) {
    server.startBeanShell(port);
  }

  public String toString() {
    if (isStarted()) {
      return "starting, startTime(" + getStartTime() + ")";
    } else if (isActive()) {
      return "active, activateTime(" + getActivateTime() + ")";
    } else {
      return "stopped";
    }
  }

  public String getVersion() {
    return productInfo.toShortString();
  }

  public String getBuildID() {
    return buildID;
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

  public int getDSOListenPort() {
    return server.getDSOListenPort();
  }

  public String[] getCpuStatNames() {
    if (cpuNames != null) return Arrays.asList(cpuNames).toArray(EMPTY_CPU_NAMES);
    if (cpuSRA == null) return cpuNames = EMPTY_CPU_NAMES;

    List list = new ArrayList();
    StatisticData[] statsData = cpuSRA.retrieveStatisticData();
    if (statsData != null) {
      for (int i = 0; i < statsData.length; i++) {
        list.add(statsData[i].getElement());
      }
    }
    return cpuNames = (String[]) list.toArray(EMPTY_CPU_NAMES);
  }

  public Map getStatistics() {
    HashMap<String, Object> map = new HashMap<String, Object>();
    MemoryUsage usage = manager.getMemoryUsage();

    map.put(MEMORY_USED, Long.valueOf(usage.getUsedMemory()));
    map.put(MEMORY_MAX, Long.valueOf(usage.getMaxMemory()));

    if (cpuSRA != null) {
      StatisticData[] statsData = getCpuUsage();
      if (statsData != null) {
        map.put(CPU_USAGE, statsData);
      }
    }

    return map;
  }

  public StatisticData[] getCpuUsage() {
    if (cpuSRA != null) { return cpuSRA.retrieveStatisticData(); }
    return null;
  }

  public String takeThreadDump(long requestMillis) {
    String text = ThreadDumpUtil.getThreadDump();
    logger.info(text);
    return text;
  }

  public String getEnvironment() {
    StringBuffer sb = new StringBuffer();
    Properties env = System.getProperties();
    Enumeration keys = env.propertyNames();
    ArrayList<String> l = new ArrayList<String>();

    while (keys.hasMoreElements()) {
      Object o = keys.nextElement();
      if (o instanceof String) {
        String key = (String) o;
        l.add(key);
      }
    }

    String[] props = l.toArray(new String[0]);
    Arrays.sort(props);
    l.clear();
    l.addAll(Arrays.asList(props));

    int maxKeyLen = 0;
    for (String key : l) {
      maxKeyLen = Math.max(key.length(), maxKeyLen);
    }

    for (String key : l) {
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
}
