/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.locks.DsoLockID;
import com.tc.object.locks.DsoVolatileLockID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

public class L2LockStatisticsManagerImpl extends LockStatisticsManager implements L2LockStatsManager, Serializable {
  private static final TCLogger             logger                   = TCLogging
                                                                         .getLogger(L2LockStatisticsManagerImpl.class);

  private volatile DSOChannelManager        channelManager;
  private ObjectStatsManager                objectStatsHelper;
  protected final Set<NodeID>               lockSpecRequestedNodeIDs = new HashSet<NodeID>();
  private SampledCounter                    globalLockCounter;
  private SampledCounter                    globalLockRecallCounter;
  private final WeakHashMap<LockID, String> lockIdToType             = new WeakHashMap<LockID, String>();

  private final static void sendLockStatisticsEnableDisableMessage(MessageChannel channel, boolean statsEnable,
                                                                   int traceDepth, int gatherInterval) {
    LockStatisticsMessage lockStatsMessage = (LockStatisticsMessage) channel
        .createMessage(TCMessageType.LOCK_STAT_MESSAGE);
    if (statsEnable) {
      lockStatsMessage.initializeEnableStat(traceDepth, gatherInterval);
    } else {
      lockStatsMessage.initializeDisableStat();
    }
    lockStatsMessage.send();
  }

  private final static void sendLockStatisticsGatheringMessage(MessageChannel channel) {
    LockStatisticsMessage lockStatsMessage = (LockStatisticsMessage) channel
        .createMessage(TCMessageType.LOCK_STAT_MESSAGE);
    lockStatsMessage.initializeLockStatisticsGathering();
    lockStatsMessage.send();
  }

  public L2LockStatisticsManagerImpl() {
    this.lockStatisticsEnabled = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.LOCK_STATISTICS_ENABLED, false);
  }

  public synchronized void start(DSOChannelManager dsoChannelManager, DSOGlobalServerStats serverStats,
                                 ObjectStatsManager objStatsHelper) {
    this.channelManager = dsoChannelManager;
    SampledCounter lockCounter = serverStats == null ? null : serverStats.getGlobalLockCounter();
    this.globalLockCounter = lockCounter == null ? SampledCounter.NULL_SAMPLED_COUNTER : lockCounter;
    SampledCounter lockRecallCounter = serverStats == null ? null : serverStats.getGlobalLockRecallCounter();
    this.globalLockRecallCounter = lockRecallCounter == null ? SampledCounter.NULL_SAMPLED_COUNTER : lockRecallCounter;
    this.objectStatsHelper = objStatsHelper;
  }

  /**
   * Abstract method implementation section begin
   */
  @Override
  protected LockStatisticsInfo newLockStatisticsContext(LockID lockID) {
    return new ServerLockStatisticsInfoImpl(lockID);
  }

  @Override
  protected void disableLockStatistics() {
    this.lockStatisticsEnabled = false;

    MessageChannel[] channels = channelManager.getActiveChannels();
    for (MessageChannel channel : channels) {
      sendLockStatisticsEnableDisableMessage(channel, false, lockStatConfig.getTraceDepth(), lockStatConfig
          .getGatherInterval());
    }
    clear();
  }

  /**
   * Abstract method implementation section ends
   */

  // We cannot synchronized on the whole method in order to prevent deadlock.
  @Override
  public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    synchronized (this) {
      super.setLockStatisticsConfig(traceDepth, gatherInterval);
    }

    sendLockStatisticsEnableDisableMessageIfNeeded(traceDepth, gatherInterval);
  }

  @Override
  public void setLockStatisticsEnabled(boolean statEnable) {
    super.setLockStatisticsEnabled(statEnable);

    sendLockStatisticsEnableDisableMessageIfNeeded(lockStatConfig.getTraceDepth(), lockStatConfig.getGatherInterval());
  }

  public synchronized boolean isLockStatisticsEnabled() {
    return this.lockStatisticsEnabled;
  }

  @Override
  public synchronized void clear() {
    super.clear();
  }

  public synchronized void recordLockHopRequested(LockID lockID) {
    globalLockRecallCounter.increment();
    if (!lockStatisticsEnabled) { return; }

    ServerLockStatisticsInfoImpl lsc = (ServerLockStatisticsInfoImpl) getLockStatInfo(lockID);
    if (lsc != null) {
      lsc.recordLockHopRequested();
    }
  }

  public synchronized void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID,
                                               int numberOfPendingRequests) {
    if (!lockStatisticsEnabled) { return; }

    String lockType = lockIdToType.get(lockID);
    if (lockType == null) {
      ObjectID objectId = null;
      if (lockID instanceof DsoLockID) {
        objectId = ((DsoLockID) lockID).getObjectID();
        lockType = this.objectStatsHelper.getObjectTypeFromID(objectId);
      } else if (lockID instanceof DsoVolatileLockID) {
        objectId = ((DsoVolatileLockID) lockID).getObjectID();
        lockType = this.objectStatsHelper.getObjectTypeFromID(objectId);
      }

      if (lockType != null) {
        lockIdToType.put(lockID, lockType);
      } else {
        lockType = "";
      }
    }

    super.recordLockRequested(lockID, nodeID, threadID, null, lockType, numberOfPendingRequests);
  }

  public synchronized void recordLockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy,
                                             long awardedTimeInMillis) {
    globalLockCounter.increment();
    if (!lockStatisticsEnabled) { return; }

    int depth = super.incrementNestedDepth(new LockKey(nodeID, threadID));
    super.recordLockAwarded(lockID, nodeID, threadID, isGreedy, awardedTimeInMillis, depth);
  }

  @Override
  public synchronized void recordLockReleased(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatisticsEnabled) { return; }

    super.decrementNestedDepth(new LockKey(nodeID, threadID));
    super.recordLockReleased(lockID, nodeID, threadID);
  }

  @Override
  public synchronized void recordLockRejected(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatisticsEnabled) { return; }

    super.recordLockRejected(lockID, nodeID, threadID);
  }

  public synchronized void recordClientStat(NodeID nodeID, Collection<TCStackTraceElement> stackTraceElements) {
    boolean nodeWaitingForStat = lockSpecRequestedNodeIDs.remove(nodeID);

    if (nodeWaitingForStat) {
      if (stackTraceElements.size() > 0) {
        for (TCStackTraceElement tcStackTraceElement : stackTraceElements) {
          LockID lockID = tcStackTraceElement.getLockID();
          LockStatElement lockStatElement = tcStackTraceElement.getLockStatElement();

          ServerLockStatisticsInfoImpl lsc = (ServerLockStatisticsInfoImpl) getOrCreateLockStatInfo(lockID);
          lsc.setLockStatElement(nodeID, lockStatElement);
        }
      }

      if (lockSpecRequestedNodeIDs.isEmpty()) {
        notifyAll();
      }
    }
  }

  public synchronized long getNumberOfLockRequested(LockID lockID) {
    if (!lockStatisticsEnabled) { return 0; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    return lsc.getNumberOfLockRequested();
  }

  public synchronized long getNumberOfLockReleased(LockID lockID) {
    if (!lockStatisticsEnabled) { return 0; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    return lsc.getNumberOfLockReleased();
  }

  public synchronized long getNumberOfPendingRequests(LockID lockID) {
    if (!lockStatisticsEnabled) { return 0; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    return lsc.getNumberOfPendingRequests();
  }

  public synchronized long getNumberOfLockHopRequests(LockID lockID) {
    if (!lockStatisticsEnabled) { return 0; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    return lsc.getNumberOfLockHopRequested();
  }

  public synchronized TimeStampedCounterValue getLockRecallMostRecentSample() {
    return globalLockRecallCounter.getMostRecentSample();
  }

  public synchronized Collection<LockSpec> getLockSpecs() {
    if (!lockStatisticsEnabled) { return Collections.EMPTY_LIST; }

    if (lockSpecRequestedNodeIDs.isEmpty()) {
      MessageChannel[] channels = channelManager.getActiveChannels();
      for (MessageChannel channel : channels) {
        sendLockStatisticsGatheringMessage(channel);
        lockSpecRequestedNodeIDs.add(new ClientID(channel.getChannelID().toLong()));
      }
    }

    try {
      while (!lockSpecRequestedNodeIDs.isEmpty()) {
        wait();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    for (Iterator<LockStatisticsInfo> i = lockStats.values().iterator(); i.hasNext();) {
      LockStatisticsInfo lsc = i.next();
      lsc.aggregateLockHoldersData();
    }
    Set<LockSpec> returnSet = new HashSet<LockSpec>(lockStats.values());
    return returnSet;
  }

  public synchronized void clearAllStatsFor(NodeID nodeID) {
    boolean statExistForNode = lockSpecRequestedNodeIDs.remove(nodeID);
    if (statExistForNode) {
      for (Iterator<ServerLockStatisticsInfoImpl> i = lockStats.values().iterator(); i.hasNext();) {
        ServerLockStatisticsInfoImpl lsc = i.next();
        lsc.clearAllStatsFor(nodeID);
      }
      if (lockSpecRequestedNodeIDs.isEmpty()) {
        notifyAll();
      }
    }
  }

  public void enableStatsForNodeIfNeeded(NodeID nodeID) {
    if (!lockStatisticsEnabled) { return; }

    try {
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      int traceDepth = getTraceDepth();
      int gatherInterval = getGatherInterval();
      sendLockStatisticsEnableDisableMessage(channel, true, traceDepth, gatherInterval);
    } catch (NoSuchChannelException e) {
      logger.warn(e);
    }
  }

  private void sendLockStatisticsEnableDisableMessageIfNeeded(int traceDepth, int gatherInterval) {
    if (isLockStatisticsEnabled()) {
      MessageChannel[] channels = channelManager.getActiveChannels();
      for (MessageChannel channel : channels) {
        sendLockStatisticsEnableDisableMessage(channel, true, traceDepth, gatherInterval);
      }
    }
  }
}
