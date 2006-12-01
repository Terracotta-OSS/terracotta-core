/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.net;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterManager;

import java.util.Map;

/**
 * A helper class to make accessing channel specific stats objects a little
 * easier. This class is sorta yucky and definitely will need to evolve
 */
public class ChannelStatsImpl implements ChannelStats {

  private static final String               DSO_STATS_MAP  = "dso_stats_map";
  private static final SampledCounterConfig DEFAULT_CONFIG = new SampledCounterConfig(1, 300, true, 0L);

  private final SampledCounterManager       counterManager;
  private final DSOChannelManager           channelManager;

  public ChannelStatsImpl(SampledCounterManager counterManager, DSOChannelManager channelManager) {
    this.counterManager = counterManager;
    this.channelManager = channelManager;
  }

  public Counter getCounter(MessageChannel channel, String name) {
    return getCounter(getStatsMap(channel), name);
  }

  private Counter getCounter(Map statsMap, String name) {
    Counter rv = (Counter) statsMap.get(name);
    if (rv != null) return rv;

    synchronized (statsMap) {
      if (statsMap.containsKey(name)) { return (Counter) statsMap.get(name); }

      // XXX: We'll need a way to override this at some point, we'll probably
      // want differing configs for the different
      // counters, and not every counter needs to be one of the sampled type
      // (probably)
      rv = counterManager.createCounter(DEFAULT_CONFIG);
      statsMap.put(name, rv);
      return rv;
    }

  }

  private static Map getStatsMap(MessageChannel channel) {
    Map rv = (Map) channel.getAttachment(DSO_STATS_MAP);
    if (rv != null) { return rv; }
    channel.addAttachment(DSO_STATS_MAP, new ConcurrentReaderHashMap(), false);
    return (Map) channel.getAttachment(DSO_STATS_MAP);
  }

  public void notifyTransaction(ChannelID channelID) {
    try {
      MessageChannel channel = channelManager.getChannel(channelID);
      getCounter(channel, TXN_RATE).increment();
    } catch (NoSuchChannelException e) {
      //
    }
  }

}
