/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.ChannelStats;
import com.tc.statistics.StatisticData;
import com.tc.stats.DSOClientMBean;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.Statistic;

import java.lang.reflect.Method;
import java.util.Map;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class DSOClient extends AbstractTerracottaMBean implements DSOClientMBean, NotificationListener {

  private static final TCLogger                logger         = TCLogging.getLogger(DSOClient.class);

  private final MBeanServer                    mbeanServer;
  private boolean                              isListeningForTunneledBeans;
  private final ObjectName                     l1InfoBeanName;
  private L1InfoMBean                          l1InfoBean;
  private final ObjectName                     instrumentationLoggingBeanName;
  private InstrumentationLoggingMBean          instrumentationLoggingBean;
  private final ObjectName                     runtimeLoggingBeanName;
  private RuntimeLoggingMBean                  runtimeLoggingBean;
  private final ObjectName                     runtimeOutputOptionsBeanName;
  private RuntimeOutputOptionsMBean            runtimeOutputOptionsBean;
  private final MessageChannel                 channel;
  private final SampledCounter                 txnRate;
  private final SampledCounter                 flushRate;
  private final SampledCounter                 faultRate;
  private final Counter                        pendingTransactions;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

  static {
    final String[] notifTypes = new String[] { TUNNELED_BEANS_REGISTERED };
    final String name = Notification.class.getName();
    final String description = "DSOClient event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final SynchronizedLong               sequenceNumber = new SynchronizedLong(0L);

  public DSOClient(final MBeanServer mbeanServer, final MessageChannel channel, final ChannelStats channelStats)
      throws NotCompliantMBeanException {
    super(DSOClientMBean.class, true);

    this.mbeanServer = mbeanServer;
    this.channel = channel;
    this.txnRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.TXN_RATE);
    this.flushRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.OBJECT_FLUSH_RATE);
    this.faultRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.OBJECT_REQUEST_RATE);
    this.pendingTransactions = channelStats.getCounter(channel, ChannelStats.PENDING_TRANSACTIONS);

    this.l1InfoBeanName = getTunneledBeanName(L1MBeanNames.L1INFO_PUBLIC);
    this.instrumentationLoggingBeanName = getTunneledBeanName(L1MBeanNames.INSTRUMENTATION_LOGGING_PUBLIC);
    this.runtimeLoggingBeanName = getTunneledBeanName(L1MBeanNames.RUNTIME_LOGGING_PUBLIC);
    this.runtimeOutputOptionsBeanName = getTunneledBeanName(L1MBeanNames.RUNTIME_OUTPUT_OPTIONS_PUBLIC);

    testSetupTunneledBeans();
  }

  private void testSetupTunneledBeans() {
    startListeningForTunneledBeans();

    if (mbeanServer.isRegistered(l1InfoBeanName)) {
      setupL1InfoBean();
    }
    if (mbeanServer.isRegistered(instrumentationLoggingBeanName)) {
      setupInstrumentationLoggingBean();
    }
    if (mbeanServer.isRegistered(runtimeLoggingBeanName)) {
      setupRuntimeLoggingBean();
    }
    if (mbeanServer.isRegistered(runtimeOutputOptionsBeanName)) {
      setupRuntimeOutputOptionsBean();
    }

    if (haveAllTunneledBeans()) {
      stopListeningForTunneledBeans();
    }
  }

  private void startListeningForTunneledBeans() {
    if (isListeningForTunneledBeans) return;
    try {
      ObjectName mbsd = new ObjectName("JMImplementation:type=MBeanServerDelegate");
      mbeanServer.addNotificationListener(mbsd, this, null, null);
    } catch (Exception e) {
      throw new RuntimeException("Adding listener to MBeanServerDelegate", e);
    }
    isListeningForTunneledBeans = true;
  }

  private void stopListeningForTunneledBeans() {
    if (!isListeningForTunneledBeans) return;
    try {
      ObjectName mbsd = new ObjectName("JMImplementation:type=MBeanServerDelegate");
      mbeanServer.removeNotificationListener(mbsd, this, null, null);
    } catch (Exception e) {
      throw new RuntimeException("Removing listener to MBeanServerDelegate", e);
    }
    isListeningForTunneledBeans = false;
  }

  public boolean isTunneledBeansRegistered() {
    return !isListeningForTunneledBeans;
  }

  public void reset() {
    // nothing to reset
  }

  private ObjectName getTunneledBeanName(ObjectName on) {
    try {
      String name = on.getCanonicalName() + ",clients=Clients,node=" + getRemoteAddress().replace(':', '/');
      return new ObjectName(name);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException("Creating ObjectName", mone);
    }
  }

  public ObjectName getL1InfoBeanName() {
    return l1InfoBeanName;
  }

  public L1InfoMBean getL1InfoBean() {
    return l1InfoBean;
  }

  public ObjectName getInstrumentationLoggingBeanName() {
    return instrumentationLoggingBeanName;
  }

  public InstrumentationLoggingMBean getInstrumentationLoggingBean() {
    return instrumentationLoggingBean;
  }

  public ObjectName getRuntimeLoggingBeanName() {
    return runtimeLoggingBeanName;
  }

  public RuntimeLoggingMBean getRuntimeLoggingBean() {
    return runtimeLoggingBean;
  }

  public ObjectName getRuntimeOutputOptionsBeanName() {
    return runtimeOutputOptionsBeanName;
  }

  public RuntimeOutputOptionsMBean getRuntimeOutputOptionsBean() {
    return runtimeOutputOptionsBean;
  }

  public ChannelID getChannelID() {
    return channel.getChannelID();
  }

  public String getRemoteAddress() {
    TCSocketAddress addr = channel.getRemoteAddress();
    if (addr == null) { return "not connected"; }
    return addr.getCanonicalStringForm();
  }

  public CountStatistic getTransactionRate() {
    return StatsUtil.makeCountStat(txnRate);
  }

  public CountStatistic getObjectFaultRate() {
    return StatsUtil.makeCountStat(faultRate);
  }

  public CountStatistic getObjectFlushRate() {
    return StatsUtil.makeCountStat(flushRate);
  }

  public CountStatistic getPendingTransactionsCount() {
    return StatsUtil.makeCountStat(pendingTransactions);
  }

  public Statistic[] getStatistics(final String[] names) {
    int count = names.length;
    Statistic[] result = new Statistic[count];
    Method method;

    for (int i = 0; i < count; i++) {
      try {
        method = getClass().getMethod("get" + names[i], new Class[] {});
        result[i] = (Statistic) method.invoke(this, new Object[] {});
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return result;
  }

  public void killClient() {
    logger.warn("Killing Client on JMX Request :" + channel);
    channel.close();
  }

  private void setupL1InfoBean() {
    l1InfoBean = (L1InfoMBean) MBeanServerInvocationHandler.newProxyInstance(mbeanServer, l1InfoBeanName,
                                                                             L1InfoMBean.class, false);
  }

  private void setupInstrumentationLoggingBean() {
    instrumentationLoggingBean = (InstrumentationLoggingMBean) MBeanServerInvocationHandler
        .newProxyInstance(mbeanServer, instrumentationLoggingBeanName, InstrumentationLoggingMBean.class, false);
  }

  private void setupRuntimeLoggingBean() {
    runtimeLoggingBean = (RuntimeLoggingMBean) MBeanServerInvocationHandler.newProxyInstance(mbeanServer,
                                                                                             runtimeLoggingBeanName,
                                                                                             RuntimeLoggingMBean.class,
                                                                                             false);
  }

  private void setupRuntimeOutputOptionsBean() {
    runtimeOutputOptionsBean = (RuntimeOutputOptionsMBean) MBeanServerInvocationHandler
        .newProxyInstance(mbeanServer, runtimeOutputOptionsBeanName, RuntimeOutputOptionsMBean.class, false);
  }

  private boolean haveAllTunneledBeans() {
    return l1InfoBean != null && instrumentationLoggingBean != null && runtimeLoggingBean != null
           && runtimeOutputOptionsBean != null;
  }

  private void beanRegistered(ObjectName beanName) {
    if (l1InfoBean == null && beanName.equals(l1InfoBeanName)) {
      setupL1InfoBean();
    }
    if (instrumentationLoggingBean == null && beanName.equals(instrumentationLoggingBeanName)) {
      setupInstrumentationLoggingBean();
    }
    if (runtimeLoggingBean == null && beanName.equals(runtimeLoggingBeanName)) {
      setupRuntimeLoggingBean();
    }
    if (runtimeOutputOptionsBean == null && beanName.equals(runtimeOutputOptionsBeanName)) {
      setupRuntimeOutputOptionsBean();
    }

    if (haveAllTunneledBeans()) {
      stopListeningForTunneledBeans();
      sendNotification(new Notification(TUNNELED_BEANS_REGISTERED, this, sequenceNumber.increment()));
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();

    if (notification instanceof MBeanServerNotification) {
      MBeanServerNotification mbsn = (MBeanServerNotification) notification;
      if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        beanRegistered(mbsn.getMBeanName());
      }
    }
  }

  /*
   * The following methods delegate to the tunneled L1InfoMBean.
   */

  public String getConfig() {
    if (l1InfoBean == null) return null;
    return l1InfoBean.getConfig();
  }

  public String[] getCpuStatNames() {
    if (l1InfoBean == null) return null;
    return l1InfoBean.getCpuStatNames();
  }

  public StatisticData[] getCpuUsage() {
    if (l1InfoBean == null) return null;
    return l1InfoBean.getCpuUsage();
  }

  public String getEnvironment() {
    if (l1InfoBean == null) return null;
    return l1InfoBean.getEnvironment();
  }

  public Map getStatistics() {
    if (l1InfoBean == null) return null;
    return l1InfoBean.getStatistics();
  }

  public String takeThreadDump(long requestMillis) {
    if (l1InfoBean == null) return null;
    return l1InfoBean.takeThreadDump(requestMillis);
  }
  
  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }
}
