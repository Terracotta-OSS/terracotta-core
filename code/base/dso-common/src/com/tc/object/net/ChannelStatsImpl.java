/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterManager;

/**
 * A helper class to make accessing channel specific stats objects a little easier. This class is sorta yucky and
 * definitely will need to evolve
 */
public class ChannelStatsImpl implements ChannelStats, DSOChannelManagerEventListener {

  private final CounterManager    counterManager;
  private final DSOChannelManager channelManager;

  public ChannelStatsImpl(CounterManager counterManager, DSOChannelManager channelManager) {
    this.counterManager = counterManager;
    this.channelManager = channelManager;
  }

  public Counter getCounter(MessageChannel channel, String name) {
    Counter rv = (Counter) channel.getAttachment(name);
    if (rv == null) { throw new NullPointerException(
                                                     "StatsMap returned null ! Probably not initialized. Check ChannelStats Interface. "); }
    return rv;
  }

  public void channelCreated(MessageChannel channel) {
    for (int i = 0; i < STATS_CONFIG.length; i++) {
      Object[] config = STATS_CONFIG[i];
      Counter counter = counterManager.createCounter(config[1]);
      channel.addAttachment((String) config[0], counter, true);
    }
  }

  public void channelRemoved(MessageChannel channel) {
    for (int i = 0; i < STATS_CONFIG.length; i++) {
      Object[] config = STATS_CONFIG[i];
      Counter counter = (Counter) channel.removeAttachment((String) config[0]);
      if (counter != null) {
        counterManager.shutdownCounter(counter);
      }
    }
  }

  public void notifyObjectRemove(MessageChannel channel, int numObjectsRemoved) {
    getCounter(channel, ChannelStats.OBJECT_FLUSH_RATE).increment(numObjectsRemoved);
  }

  public void notifyObjectRequest(MessageChannel channel, int numObjectsRequested) {
    getCounter(channel, ChannelStats.OBJECT_REQUEST_RATE).increment(numObjectsRequested);
  }

  public void notifyTransaction(NodeID nodeID) {
    try {
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      getCounter(channel, TXN_RATE).increment();
    } catch (NoSuchChannelException e) {
      //
    }
  }

  public void notifyTransactionBroadcastedTo(NodeID nodeID) {
    try {
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      getCounter(channel, PENDING_TRANSACTIONS).increment();
    } catch (NoSuchChannelException e) {
      //
    }
  }

  public void notifyTransactionAckedFrom(NodeID nodeID) {
    try {
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      getCounter(channel, PENDING_TRANSACTIONS).decrement();
    } catch (NoSuchChannelException e) {
      //
    }
  }

}
