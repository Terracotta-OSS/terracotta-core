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
import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.l1.api.ClientStateManager;
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
import javax.management.relation.MBeanServerNotificationFilter;

public class DSOClient extends AbstractTerracottaMBean implements DSOClientMBean, NotificationListener {

  private static final TCLogger                logger                  = TCLogging.getLogger(DSOClient.class);

  private final MBeanServer                    mbeanServer;
  private boolean                              isListeningForTunneledBeans;
  private ObjectName                           l1InfoBeanName;
  private L1InfoMBean                          l1InfoBean;
  private final ObjectName                     l1DumperBeanName;
  private ObjectName                           l1OperatorEventsBeanName;
  private TerracottaOperatorEventsMBean        l1OperatorEventsBean;
  private final MessageChannel                 channel;
  private final SampledCounter                 txnRate;
  private final SampledCounter                 writeRate;
  private final SampledCounter                 readRate;
  private final Counter                        pendingTransactions;
  private final AtomicLong                     sequenceNumber          = new AtomicLong(0L);
  private final SampledCumulativeCounter       serverMapGetSizeRequestsCounter;
  private final SampledCumulativeCounter       serverMapGetValueRequestsCounter;
  private final ClientID                       clientID;
  private final ClientStateManager             stateManager;

  private ObjectName                           enterpriseMBeanName;
  private boolean                              isEnterpriseBeanNameSet = false;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

  private final MBeanRegistrationFilter        registrationFilter;

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
    this.writeRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.WRITE_RATE);
    this.readRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.READ_RATE);
    this.pendingTransactions = channelStats.getCounter(channel, ChannelStats.PENDING_TRANSACTIONS);
    this.serverMapGetSizeRequestsCounter = (SampledCumulativeCounter) channelStats
        .getCounter(channel, ChannelStats.SERVER_MAP_GET_SIZE_REQUESTS);
    this.serverMapGetValueRequestsCounter = (SampledCumulativeCounter) channelStats
        .getCounter(channel, ChannelStats.SERVER_MAP_GET_VALUE_REQUESTS);

    this.l1InfoBeanName = getTunneledBeanName(L1MBeanNames.L1INFO_PUBLIC);
    this.l1OperatorEventsBeanName = getTunneledBeanName(MBeanNames.OPERATOR_EVENTS_PUBLIC);
    this.enterpriseMBeanName = getTunneledBeanName(L1MBeanNames.ENTERPRISE_TC_CLIENT);
    this.l1DumperBeanName = getTunneledBeanName(MBeanNames.L1DUMPER_INTERNAL);

    try {
      this.registrationFilter = new MBeanRegistrationFilter(getTunneledBeanName(new ObjectName("*:*")));
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    }

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

    if ((beanName = queryClientBean(this.l1OperatorEventsBeanName)) != null) {
      this.l1OperatorEventsBeanName = beanName;
      setupL1OperatorEventsBean();
    }

    if (haveAllTunneledBeans()) {
      stopListeningForTunneledBeans();
    }
  }

  private static class MBeanRegistrationFilter extends MBeanServerNotificationFilter {
    static final long        serialVersionUID = 42L;

    private final ObjectName pattern;

    private MBeanRegistrationFilter(ObjectName pattern) {
      this.pattern = pattern;
    }

    @Override
    public synchronized boolean isNotificationEnabled(Notification notif) {
      if (notif instanceof MBeanServerNotification) {
        MBeanServerNotification mbsn = (MBeanServerNotification) notif;
        return pattern.apply(mbsn.getMBeanName());
      }
      return false;
    }
  }

  private void startListeningForTunneledBeans() {
    if (isListeningForTunneledBeans) return;
    try {
      mbeanServer.addNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this,
                                          registrationFilter, null);
    } catch (Exception e) {
      throw new RuntimeException("Adding listener to MBeanServerDelegate", e);
    }
    isListeningForTunneledBeans = true;
  }

  void stopListeningForTunneledBeans() {
    if (!isListeningForTunneledBeans) return;
    try {
      mbeanServer.removeNotificationListener(new ObjectName("JMImplementation:type=MBeanServerDelegate"), this,
                                             registrationFilter, null);
    } catch (Exception e) {
      throw new RuntimeException("Removing listener to MBeanServerDelegate", e);
    }
    isListeningForTunneledBeans = false;
  }

  @Override
  public boolean isTunneledBeansRegistered() {
    return !isListeningForTunneledBeans;
  }

  @Override
  public void reset() {
    // nothing to reset
  }

  public ObjectName getTunneledBeanName(ObjectName on) {
    try {
      String name = on.getCanonicalName() + ",clients=Clients,node=" + getRemoteAddress().replace(':', '_');
      return new ObjectName(name);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException("Creating ObjectName", mone);
    }
  }

  @Override
  public long getClientID() {
    return clientID.toLong();
  }

  /**
   * This method returns the same String as the parameter to thisNodeConnected() call in cluster JMX event.
   */
  @Override
  public String getNodeID() {
    return clientID.toString();
  }

  @Override
  public ObjectName getL1InfoBeanName() {
    return l1InfoBeanName;
  }

  @Override
  public L1InfoMBean getL1InfoBean() {
    return l1InfoBean;
  }

  @Override
  public ObjectName getL1DumperBeanName() {
    return this.l1DumperBeanName;
  }

  @Override
  public ObjectName getL1OperatorEventsBeanName() {
    return l1OperatorEventsBeanName;
  }

  @Override
  public TerracottaOperatorEventsMBean getL1OperatorEventsBean() {
    return l1OperatorEventsBean;
  }

  @Override
  public ChannelID getChannelID() {
    return channel.getChannelID();
  }

  @Override
  public String getRemoteAddress() {
    TCSocketAddress addr = channel.getRemoteAddress();
    if (addr == null) { return "not connected"; }
    return addr.getCanonicalStringForm();
  }

  @Override
  public long getTransactionRate() {
    return txnRate.getMostRecentSample().getCounterValue();
  }

  @Override
  public long getReadRate() {
    return readRate.getMostRecentSample().getCounterValue();
  }

  @Override
  public long getWriteRate() {
    return writeRate.getMostRecentSample().getCounterValue();
  }

  @Override
  public long getPendingTransactionsCount() {
    return pendingTransactions.getValue();
  }

  @Override
  public long getServerMapGetSizeRequestsCount() {
    return serverMapGetSizeRequestsCounter.getCumulativeValue();
  }

  @Override
  public long getServerMapGetSizeRequestsRate() {
    return serverMapGetSizeRequestsCounter.getMostRecentSample().getCounterValue();
  }

  @Override
  public long getServerMapGetValueRequestsCount() {
    return serverMapGetValueRequestsCounter.getCumulativeValue();
  }

  @Override
  public long getServerMapGetValueRequestsRate() {
    return serverMapGetValueRequestsCounter.getMostRecentSample().getCounterValue();
  }

  @Override
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

  @Override
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

  private boolean haveAllTunneledBeans() {
    return l1InfoBean != null;
  }

  /**
   * Since ObjectNames can have arbitrary attribute pairs, we need to match against a wildcard pattern that we expect.
   * Each tunneled client bean is uniquely identified by its node attribute, which is constructed from the remote host
   * and port of the terracotta client.
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
    if (!haveAllTunneledBeans()) {
      if (l1InfoBean == null && matchesClientBeanName(l1InfoBeanName, beanName)) {
        l1InfoBeanName = beanName;
        setupL1InfoBean();
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
  }

  @Override
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

  @Override
  public int getLiveObjectCount() {
    return stateManager.getReferenceCount(clientID);
  }

  @Override
  public boolean isResident(ObjectID oid) {
    return stateManager.hasReference(clientID, oid);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return Arrays.asList(NOTIFICATION_INFO).toArray(EMPTY_NOTIFICATION_INFO);
  }

  @Override
  public ObjectName getEnterpriseTCClientBeanName() {
    return enterpriseMBeanName;
  }
}
