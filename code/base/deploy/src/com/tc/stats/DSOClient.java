/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.ChannelStats;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.statistics.CountStatistic;
import com.tc.stats.statistics.Statistic;

import java.lang.reflect.Method;

import javax.management.NotCompliantMBeanException;

public class DSOClient extends AbstractTerracottaMBean implements DSOClientMBean {

  private static final TCLogger logger = TCLogging.getLogger(DSOClient.class);

  private final MessageChannel  channel;
  private final SampledCounter  txnRate;
  private final SampledCounter  flushRate;
  private final SampledCounter  faultRate;
  private final Counter         pendingTransactions;

  public DSOClient(final MessageChannel channel, final ChannelStats channelStats) throws NotCompliantMBeanException {
    super(DSOClientMBean.class, false);
    this.channel = channel;
    this.txnRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.TXN_RATE);
    this.flushRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.OBJECT_FLUSH_RATE);
    this.faultRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.OBJECT_REQUEST_RATE);
    this.pendingTransactions = channelStats.getCounter(channel, ChannelStats.PENDING_TRANSACTIONS);
  }

  public void reset() {
    // nothing to reset
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
}
