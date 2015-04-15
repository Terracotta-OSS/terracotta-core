/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.impl;

import com.google.common.eventbus.Subscribe;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ServerMapRequestType;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.net.NoSuchChannelException;
import com.tc.stats.StatsConfig;
import com.tc.stats.counter.BoundedCounterConfig;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCumulativeCounterConfig;
import com.tc.util.Events;

/**
 * A helper class to make accessing channel specific stats objects a little easier. This class is sorta yucky and
 * definitely will need to evolve
 */
public class ChannelStatsImpl implements ChannelStats, DSOChannelManagerEventListener {

  private static final StatsConfig[] STATS_CONFIG = new StatsConfig[] {
      new StatsConfig(READ_RATE, new SampledCounterConfig(1, 300, true, 0L)),
      new StatsConfig(WRITE_RATE, new SampledCounterConfig(1, 300, true, 0L)),
      new StatsConfig(TXN_RATE, new SampledCounterConfig(1, 300, true, 0L)),
      new StatsConfig(PENDING_TRANSACTIONS, new BoundedCounterConfig(0L, 0L, Long.MAX_VALUE)),
      new StatsConfig(SERVER_MAP_GET_SIZE_REQUESTS, new SampledCumulativeCounterConfig(1, 300, true, 0L)),
      new StatsConfig(SERVER_MAP_GET_VALUE_REQUESTS, new SampledCumulativeCounterConfig(1, 300, true, 0L)) };

  private final CounterManager    counterManager;
  private final DSOChannelManager channelManager;

  public ChannelStatsImpl(CounterManager counterManager, DSOChannelManager channelManager) {
    this.counterManager = counterManager;
    this.channelManager = channelManager;
  }

  @Override
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

  @Override
  public void channelCreated(MessageChannel channel) {
    // NOP
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    for (StatsConfig config : STATS_CONFIG) {
      Counter counter = (Counter) channel.removeAttachment(config.getStatsName());
      if (counter != null) {
        counterManager.shutdownCounter(counter);
      }
    }
  }

  @Subscribe
  public void writeOperationEvent(Events.WriteOperationCountChangeEvent event) {
    try {
      MessageChannel channel = channelManager.getActiveChannel(event.getSource());
      getCounter(channel, WRITE_RATE).increment(event.getDelta());
    } catch (NoSuchChannelException e) {
      //
    }
  }

  @Override
  public void notifyReadOperations(MessageChannel channel, int numObjectsRequested) {
    getCounter(channel, ChannelStats.READ_RATE).increment(numObjectsRequested);
  }

  @Override
  public void notifyTransaction(NodeID nodeID, int numTxns) {
    try {
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      getCounter(channel, TXN_RATE).increment(numTxns);
    } catch (NoSuchChannelException e) {
      //
    }
  }

  @Override
  public void notifyTransactionBroadcastedTo(NodeID nodeID) {
    try {
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      getCounter(channel, PENDING_TRANSACTIONS).increment();
    } catch (NoSuchChannelException e) {
      //
    }
  }

  @Override
  public void notifyTransactionAckedFrom(NodeID nodeID) {
    try {
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      getCounter(channel, PENDING_TRANSACTIONS).decrement();
    } catch (NoSuchChannelException e) {
      //
    }
  }

  @Override
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
