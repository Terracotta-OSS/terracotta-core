/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.stats;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.handshakemanager.ClientHandshakeMonitoringInfo;
import com.tc.stats.api.ClientMBean;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.SampledCounter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
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

public class Client extends AbstractTerracottaMBean implements ClientMBean, NotificationListener {

  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  private final MBeanServer                    mbeanServer;
  private ObjectName                           l1InfoBeanName;
  private L1InfoMBean                          l1InfoBean;
  private final MessageChannel                 channel;
  private final SampledCounter                 txnRate;
  private final SampledCounter                 writeRate;
  private final SampledCounter                 readRate;
  private final Counter                        pendingTransactions;
  private final ClientHandshakeMonitoringInfo   minfo;
  private final ClientID                       clientID;

  private ObjectName                           enterpriseMBeanName;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

  static {
    final String[] notifTypes = new String[] { };
    final String name = Notification.class.getName();
    final String description = "DSOClient event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  public Client(MBeanServer mbeanServer, MessageChannel channel, ChannelStats channelStats,
                ClientID clientID) throws NotCompliantMBeanException {
    super(ClientMBean.class, true);

    this.mbeanServer = mbeanServer;
    this.channel = channel;
    this.clientID = clientID;
    this.txnRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.TXN_RATE);
    this.writeRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.WRITE_RATE);
    this.readRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.READ_RATE);
    this.pendingTransactions = channelStats.getCounter(channel, ChannelStats.PENDING_TRANSACTIONS);
    this.minfo = (ClientHandshakeMonitoringInfo)channel.getAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT);

  }

  @Override
  public void reset() {
    // nothing to reset
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
    return ObjectName.WILDCARD;
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
  public Number[] getStatistics(String[] names) {
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
  
  @Override
  public int getRemotePID() {
    if (minfo !=null) {
      return minfo.getPid();
    }
    return -1;
  }
  @Override
  public String getRemoteName() {
    if (minfo !=null) {
      return minfo.getName();
    }
    return "";
  }
  @Override
  public String getRemoteUUID() {
    if (minfo !=null) {
      return minfo.getUuid();
    }
    return "";
  }
  
  private void setupL1InfoBean() {
    l1InfoBean = MBeanServerInvocationHandler.newProxyInstance(mbeanServer, l1InfoBeanName, L1InfoMBean.class, false);
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

  public Map<String, Object> getStatistics() {
    if (l1InfoBean == null) return null;
    return l1InfoBean.getStatistics();
  }

  public String takeThreadDump(long requestMillis) {
    if (l1InfoBean == null) return null;
    return l1InfoBean.takeThreadDump(requestMillis);
  }

  @Override
  public int getLiveObjectCount() {
    return -1;
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
