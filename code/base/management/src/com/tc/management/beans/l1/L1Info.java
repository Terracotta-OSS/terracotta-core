/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.l1;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

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
import java.util.Timer;
import java.util.TimerTask;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

public class L1Info extends AbstractTerracottaMBean implements L1InfoMBean {
  private static final TCLogger                logger                         = TCLogging.getLogger(L1Info.class);

  private static final String                  STATISTICS_TYPE                = "l1.statistics";

  private static final long                    STATISTICS_NOTIFICATION_MILLIS = 2000;

  private final String                         rawConfigText;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

  static {
    final String[] notifTypes = new String[] { STATISTICS_TYPE };
    final String name = Notification.class.getName();
    final String description = "L1Info event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final SynchronizedLong               sequenceNumber                 = new SynchronizedLong(0L);
  private Timer                                statsEmitterTimer;

  private final JVMMemoryManager               manager;
  private StatisticRetrievalAction             cpuSRA;

  public L1Info(String rawConfigText) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, true);
    manager = TCRuntime.getJVMMemoryManager();
    this.rawConfigText = rawConfigText;
    try {
      Class sraCpuType = Class.forName("com.tc.statistics.retrieval.actions.SRACpuCombined");
      if (sraCpuType != null) {
        cpuSRA = (StatisticRetrievalAction) sraCpuType.newInstance();
        logger.info("L1 got SRACpuCombined");
      }
    } catch (Exception e) {
      /**/
    }
  }

  private void testStartStatsEmitter() {
    if (statsEmitterTimer == null && hasListeners()) {
      statsEmitterTimer = new Timer();
      statsEmitterTimer.scheduleAtFixedRate(new StatsEmitterTimerTask(), 1000, STATISTICS_NOTIFICATION_MILLIS);
      logger.info("Started L1 stats emitter timer");
    }
  }

  private void testStopStatsEmitter() {
    if (!hasListeners()) {
      statsEmitterTimer.cancel();
      statsEmitterTimer = null;
      logger.info("Stopped L1 stats emitter timer");
    }
  }

  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter,
                                      final Object obj) {
    super.addNotificationListener(listener, filter, obj);
    // testStartStatsEmitter();
  }

  public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter,
                                         final Object obj) throws ListenerNotFoundException {
    super.removeNotificationListener(listener, filter, obj);
    // testStopStatsEmitter();
  }

  public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    super.removeNotificationListener(listener);
    // testStopStatsEmitter();
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

    // TODO: if current stats session, store thread dump text at moment requestMillis.

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

  class StatsEmitterTimerTask extends TimerTask {
    public void run() {
      if (!hasListeners()) return;
      Notification notif = new Notification(STATISTICS_TYPE, L1Info.this, sequenceNumber.increment(), System
          .currentTimeMillis());
      notif.setUserData(getStatistics());
      sendNotification(notif);
    }
  }

  public void reset() {
    /**/
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

}
