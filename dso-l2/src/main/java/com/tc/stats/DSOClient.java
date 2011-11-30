/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.beans.MBeanNames;
import com.tc.management.beans.TerracottaOperatorEventsMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.statistics.StatisticData;
import com.tc.stats.api.DSOClientMBean;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

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

  private static final TCLogger                logger                  = TCLogging.getLogger(DSOClient.class);

  private final MBeanServer                    mbeanServer;
  private boolean                              isListeningForTunneledBeans;
  private ObjectName                           l1InfoBeanName;
  private L1InfoMBean                          l1InfoBean;
  private final ObjectName                     l1DumperBeanName;
  private ObjectName                           instrumentationLoggingBeanName;
  private InstrumentationLoggingMBean          instrumentationLoggingBean;
  private ObjectName                           runtimeLoggingBeanName;
  private RuntimeLoggingMBean                  runtimeLoggingBean;
  private ObjectName                           runtimeOutputOptionsBeanName;
  private RuntimeOutputOptionsMBean            runtimeOutputOptionsBean;
  private ObjectName                           l1OperatorEventsBeanName;
  private TerracottaOperatorEventsMBean        l1OperatorEventsBean;
  private final MessageChannel                 channel;
  private final SampledCounter                 txnRate;
  private final SampledCounter                 flushRate;
  private final SampledCounter                 faultRate;
  private final Counter                        pendingTransactions;
  private final AtomicLong                     sequenceNumber          = new AtomicLong(0L);
  private final SampledCumulativeCounter       serverMapGetSizeRequestsCounter;
  private final SampledCumulativeCounter       serverMapGetValueRequestsCounter;
  private final ClientID                       clientID;
  private final ClientStateManager             stateManager;

  private ObjectName                           enterpriseMBeanName;
  private boolean                              isEnterpriseBeanNameSet = false;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

  static {
    final String[] notifTypes = new String[] { TUNNELED_BEANS_REGISTERED };
    final String name = Notification.class.getName();
    final String description = "DSOClient event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  public DSOClient(final MBeanServer mbeanServer, final MessageChannel channel, final ChannelStats channelStats,
                   ClientID clientID, ClientStateManager stateManager) throws NotCompliantMBeanException {
    super(DSOClientMBean.class, true);

    this.mbeanServer = mbeanServer;
    this.channel = channel;
    this.clientID = clientID;
    this.stateManager = stateManager;
    this.txnRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.TXN_RATE);
    this.flushRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.OBJECT_FLUSH_RATE);
    this.faultRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.OBJECT_REQUEST_RATE);
    this.pendingTransactions = channelStats.getCounter(channel, ChannelStats.PENDING_TRANSACTIONS);
    this.serverMapGetSizeRequestsCounter = (SampledCumulativeCounter) channelStats
        .getCounter(channel, ChannelStats.SERVER_MAP_GET_SIZE_REQUESTS);
    this.serverMapGetValueRequestsCounter = (SampledCumulativeCounter) channelStats
        .getCounter(channel, ChannelStats.SERVER_MAP_GET_VALUE_REQUESTS);

    this.l1InfoBeanName = getTunneledBeanName(L1MBeanNames.L1INFO_PUBLIC);
    this.instrumentationLoggingBeanName = getTunneledBeanName(L1MBeanNames.INSTRUMENTATION_LOGGING_PUBLIC);
    this.runtimeLoggingBeanName = getTunneledBeanName(L1MBeanNames.RUNTIME_LOGGING_PUBLIC);
    this.runtimeOutputOptionsBeanName = getTunneledBeanName(L1MBeanNames.RUNTIME_OUTPUT_OPTIONS_PUBLIC);
    this.l1OperatorEventsBeanName = getTunneledBeanName(MBeanNames.OPERATOR_EVENTS_PUBLIC);
    this.enterpriseMBeanName = getTunneledBeanName(L1MBeanNames.ENTERPRISE_TC_CLIENT);
    this.l1DumperBeanName = getTunneledBeanName(MBeanNames.L1DUMPER_INTERNAL);

    testSetupTunneledBeans();
  }

  /**
   * The tunneled client bean names must be queried-for using a wildcard pattern since they can have attributes
   * (tc.node-name, others may come later) in addition to those expected. Each of the prototype names created match a
   * single tunneled bean, uniquely identified by its node attribute. The initial tunneled bean names are not patterns.
   */
  private ObjectName queryClientBean(ObjectName o) {
    try {
      ObjectName pattern = new ObjectName(o.getCanonicalName() + ",*");
      Set result = mbeanServer.queryNames(pattern, null);
      Iterator iter = result.iterator();
      return iter.hasNext() ? (ObjectName) iter.next() : null;
    } catch (MalformedObjectNameException moe) {
      throw new RuntimeException(moe);
    }
  }

  private void testSetupTunneledBeans() {
    startListeningForTunneledBeans();

    ObjectName beanName;

    if ((beanName = queryClientBean(l1InfoBeanName)) != null) {
      l1InfoBeanName = beanName;
      setupL1InfoBean();
    }

    if ((beanName = queryClientBean(instrumentationLoggingBeanName)) != null) {
      instrumentationLoggingBeanName = beanName;
      setupInstrumentationLoggingBean();
    }

    if ((beanName = queryClientBean(runtimeLoggingBeanName)) != null) {
      runtimeLoggingBeanName = beanName;
      setupRuntimeLoggingBean();
    }

    if ((beanName = queryClientBean(runtimeOutputOptionsBeanName)) != null) {
      runtimeOutputOptionsBeanName = beanName;
      setupRuntimeOutputOptionsBean();
    }

    if ((beanName = queryClientBean(this.l1OperatorEventsBeanName)) != null) {
      this.l1OperatorEventsBeanName = beanName;
      setupL1OperatorEventsBean();
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

  public ObjectName getTunneledBeanName(ObjectName on) {
    try {
      String name = on.getCanonicalName() + ",clients=Clients,node=" + getRemoteAddress().replace(':', '/');
      return new ObjectName(name);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException("Creating ObjectName", mone);
    }
  }

  public ClientID getClientID() {
    return clientID;
  }

  /**
   * This method returns the same String as the parameter to thisNodeConnected() call in cluster JMX event.
   */
  public String getNodeID() {
    return clientID.toString();
  }

  public ObjectName getL1InfoBeanName() {
    return l1InfoBeanName;
  }

  public L1InfoMBean getL1InfoBean() {
    return l1InfoBean;
  }

  public ObjectName getL1DumperBeanName() {
    return this.l1DumperBeanName;
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

  public ObjectName getL1OperatorEventsBeanName() {
    return l1OperatorEventsBeanName;
  }

  public TerracottaOperatorEventsMBean getL1OperatorEventsBean() {
    return l1OperatorEventsBean;
  }

  public ChannelID getChannelID() {
    return channel.getChannelID();
  }

  public String getRemoteAddress() {
    TCSocketAddress addr = channel.getRemoteAddress();
    if (addr == null) { return "not connected"; }
    return addr.getCanonicalStringForm();
  }

  public long getTransactionRate() {
    return txnRate.getMostRecentSample().getCounterValue();
  }

  public long getObjectFaultRate() {
    return faultRate.getMostRecentSample().getCounterValue();
  }

  public long getObjectFlushRate() {
    return flushRate.getMostRecentSample().getCounterValue();
  }

  public long getPendingTransactionsCount() {
    return pendingTransactions.getValue();
  }

  public long getServerMapGetSizeRequestsCount() {
    return serverMapGetSizeRequestsCounter.getCumulativeValue();
  }

  public long getServerMapGetSizeRequestsRate() {
    return serverMapGetSizeRequestsCounter.getMostRecentSample().getCounterValue();
  }

  public long getServerMapGetValueRequestsCount() {
    return serverMapGetValueRequestsCounter.getCumulativeValue();
  }

  public long getServerMapGetValueRequestsRate() {
    return serverMapGetValueRequestsCounter.getMostRecentSample().getCounterValue();
  }

  public Number[] getStatistics(final String[] names) {
    int count = names.length;
    Number[] result = new Number[count];
    Method method;

    for (int i = 0; i < count; i++) {
      try {
        method = getClass().getMethod("get" + names[i], new Class[] {});
        result[i] = (Number) method.invoke(this, new Object[] {});
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
    l1InfoBean = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, l1InfoBeanName, L1InfoMBean.class, false);
  }

  private void setupL1OperatorEventsBean() {
    this.l1OperatorEventsBean = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, l1OperatorEventsBeanName,
                                                                              TerracottaOperatorEventsMBean.class,
                                                                              false);
  }

  private void setupInstrumentationLoggingBean() {
    instrumentationLoggingBean = MBeanServerInvocationHandler
        .newProxyInstance(mbeanServer, instrumentationLoggingBeanName, InstrumentationLoggingMBean.class, false);
  }

  private void setupRuntimeLoggingBean() {
    runtimeLoggingBean = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, runtimeLoggingBeanName,
                                                                       RuntimeLoggingMBean.class, false);
  }

  private void setupRuntimeOutputOptionsBean() {
    runtimeOutputOptionsBean = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, runtimeOutputOptionsBeanName,
                                                                             RuntimeOutputOptionsMBean.class, false);
  }

  private boolean haveAllTunneledBeans() {
    return l1InfoBean != null && instrumentationLoggingBean != null && runtimeLoggingBean != null
           && runtimeOutputOptionsBean != null;
  }

  /**
   * Since ObjectNames can have arbitrary attribute pairs, we need to match against a wildcard pattern that we expect.
   * Each tunneled client bean is uniquely identified by its node attribute, which is constructed from the remote host
   * and port of the DSO client.
   */
  private boolean matchesClientBeanName(ObjectName clientBeanName, ObjectName beanName) {
    try {
      ObjectName wildcard = new ObjectName(clientBeanName.getCanonicalName() + ",*");
      return wildcard.apply(beanName);
    } catch (MalformedObjectNameException moe) {
      throw new RuntimeException(moe);
    }
  }

  private void beanRegistered(ObjectName beanName) {
    if (l1InfoBean == null && matchesClientBeanName(l1InfoBeanName, beanName)) {
      l1InfoBeanName = beanName;
      setupL1InfoBean();
    }

    if (instrumentationLoggingBean == null && matchesClientBeanName(instrumentationLoggingBeanName, beanName)) {
      instrumentationLoggingBeanName = beanName;
      setupInstrumentationLoggingBean();
    }

    if (runtimeLoggingBean == null && matchesClientBeanName(runtimeLoggingBeanName, beanName)) {
      runtimeLoggingBeanName = beanName;
      setupRuntimeLoggingBean();
    }

    if (runtimeOutputOptionsBean == null && matchesClientBeanName(runtimeOutputOptionsBeanName, beanName)) {
      runtimeOutputOptionsBeanName = beanName;
      setupRuntimeOutputOptionsBean();
    }

    if (!isEnterpriseBeanNameSet && matchesClientBeanName(enterpriseMBeanName, beanName)) {
      enterpriseMBeanName = beanName;
      isEnterpriseBeanNameSet = true;
    }

    if (haveAllTunneledBeans()) {
      stopListeningForTunneledBeans();
      sendNotification(new Notification(TUNNELED_BEANS_REGISTERED, this, sequenceNumber.incrementAndGet()));
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

  public int getLiveObjectCount() {
    return stateManager.getReferenceCount(clientID);
  }

  public boolean isResident(ObjectID oid) {
    return stateManager.hasReference(clientID, oid);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return Arrays.asList(NOTIFICATION_INFO).toArray(EMPTY_NOTIFICATION_INFO);
  }

  public ObjectName getEnterpriseTCClientBeanName() {
    return enterpriseMBeanName;
  }
}
