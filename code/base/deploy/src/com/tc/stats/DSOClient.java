package com.tc.stats;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.ChannelStats;
import com.tc.stats.counter.sampled.SampledCounter;

import java.lang.reflect.Method;

import javax.management.NotCompliantMBeanException;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.Statistic;

public class DSOClient extends AbstractTerracottaMBean implements DSOClientMBean {

  private final MessageChannel channel;
  private final SampledCounter txnRate;
  private final SampledCounter flushRate;
  private final SampledCounter faultRate;

  public DSOClient(final MessageChannel channel, final ChannelStats channelStats) throws NotCompliantMBeanException {
    super(DSOClientMBean.class, false);
    this.channel = channel;
    this.txnRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.TXN_RATE);
    this.flushRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.OBJECT_FLUSH_RATE);
    this.faultRate = (SampledCounter) channelStats.getCounter(channel, ChannelStats.OBJECT_REQUEST_RATE);
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
    return addr.getStringForm();
  }

  public CountStatistic getTransactionRate() {
    return StatsUtil.makeCountStat(txnRate, "Transaction rate", "transactions per second",
                                   "Total transactions per second for this client");
  }

  public CountStatistic getObjectFaultRate() {
    return StatsUtil.makeCountStat(faultRate, "Object fault rate", "objects per second",
                                   "Total objects faulted per second for this client");
  }

  public CountStatistic getObjectFlushRate() {
    return StatsUtil.makeCountStat(flushRate, "Object flush rate", "objects per second",
                                   "Total objects flushed per second for this client");
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

}
