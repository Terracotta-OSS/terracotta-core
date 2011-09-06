/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ServerMapRequestType;
import com.tc.stats.StatsConfig;
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
    if (rv == null) {
      createStatsCountersIfNeeded(channel, name);
      rv = (Counter) channel.getAttachment(name);
      if (rv == null) throw new NullPointerException("StatsCounter : " + name + " not attached to channel "
                                                     + channel.getChannelID()
                                                     + " ! Probably not initialized. Check ChannelStats Interface. ");
    }
    return rv;
  }

  private synchronized void createStatsCountersIfNeeded(MessageChannel channel, String name) {
    Counter rv = (Counter) channel.getAttachment(name);
    if (rv == null) {
      for (StatsConfig config : STATS_CONFIG) {
        Counter counter = counterManager.createCounter(config.getCounterConfig());
        channel.addAttachment(config.getStatsName(), counter, true);
      }
    }
  }

  public void channelCreated(MessageChannel channel) {
    // NOP
  }

  public void channelRemoved(MessageChannel channel) {
    for (StatsConfig config : STATS_CONFIG) {
      Counter counter = (Counter) channel.removeAttachment(config.getStatsName());
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

  public void notifyTransaction(NodeID nodeID, int numTxns) {
    try {
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      getCounter(channel, TXN_RATE).increment(numTxns);
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

  public void notifyServerMapRequest(final ServerMapRequestType type, final MessageChannel channel,
                                     final int numRequests) {
    Counter counter = null;
    switch (type) {
      case GET_SIZE:
        counter = getCounter(channel, ChannelStats.SERVER_MAP_GET_SIZE_REQUESTS);
        break;
      case GET_VALUE_FOR_KEY:
        counter = getCounter(channel, ChannelStats.SERVER_MAP_GET_VALUE_REQUESTS);
        break;

      default:
        break;
    }
    if (counter != null) {
      counter.increment(numRequests);
    }
  }

}
