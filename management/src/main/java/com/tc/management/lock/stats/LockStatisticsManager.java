/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.net.NodeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ThreadID;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Counter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Methods in this class are not synchronized because they are called from the context of its concrete subclasses.
 */
public abstract class LockStatisticsManager implements Serializable {
  protected final ConcurrentHashMap lockStats      = new ConcurrentHashMap(); // map<lockID, LockStatisticsInfo>
  protected final LockStatConfig    lockStatConfig = new LockStatConfig();
  protected final Map               nestedDepth    = new HashMap();          // map<ThreadID/NodeID, int>

  protected volatile boolean        lockStatisticsEnabled;

  public void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, StackTraceElement[] stackTraces,
                                  String contextInfo, int numberOfPendingRequests) {
    if (!lockStatisticsEnabled) { return; }

    LockStatisticsInfo lsc = getOrCreateLockStatInfo(lockID);
    lsc.recordLockRequested(nodeID, threadID, System.currentTimeMillis(), numberOfPendingRequests, stackTraces,
                            contextInfo);
  }

  public boolean recordLockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy,
                                   long awardedTimeInMillis, int nestedLockDepth) {
    if (!lockStatisticsEnabled) { return false; }

    LockStatisticsInfo lsc = getLockStatInfo(lockID);
    if (lsc != null) { return lsc.recordLockAwarded(nodeID, threadID, isGreedy, awardedTimeInMillis, nestedLockDepth); }
    return false;
  }

  public void recordLockRejected(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatisticsEnabled) { return; }

    LockStatisticsInfo lsc = getLockStatInfo(lockID);
    if (lsc != null) {
      lsc.recordLockRejected(nodeID, threadID);
    }
  }

  public void recordLockReleased(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatisticsEnabled) { return; }

    LockStatisticsInfo lsc = getLockStatInfo(lockID);
    if (lsc != null) {
      lsc.recordLockReleased(nodeID, threadID);
    }
  }

  public void clear() {
    this.lockStats.clear();
  }

  public synchronized int getTraceDepth() {
    return lockStatConfig.getTraceDepth();
  }

  public synchronized int getGatherInterval() {
    return lockStatConfig.getGatherInterval();
  }

  public synchronized void setLockStatisticsEnabled(boolean statEnable) {
    if ((this.lockStatisticsEnabled = statEnable) == false) {
      disableLockStatistics();
    }
  }

  public Map getLockStats() {
    return lockStats;
  }

  protected abstract void disableLockStatistics();

  protected abstract LockStatisticsInfo newLockStatisticsContext(LockID lockID);

  protected void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    clear();
    lockStatConfig.setConfig(traceDepth, gatherInterval);
  }

  protected LockStatisticsInfo getLockStatInfo(LockID lockID) {
    return (LockStatisticsInfo) lockStats.get(lockID);
  }

  protected LockStatisticsInfo getOrCreateLockStatInfo(LockID lockID) {
    LockStatisticsInfo lsc = (LockStatisticsInfo) lockStats.get(lockID);
    if (lsc == null) {
      lsc = newLockStatisticsContext(lockID);
      LockStatisticsInfo existLsc = (LockStatisticsInfo) lockStats.putIfAbsent(lockID, lsc);
      if (existLsc != null) return existLsc;
    }
    return lsc;
  }

  protected int incrementNestedDepth(Object depthTrackingKey) {
    Counter depth = (Counter) nestedDepth.get(depthTrackingKey);
    if (depth == null) {
      depth = new Counter(1);
      nestedDepth.put(depthTrackingKey, depth);
    } else {
      depth.increment();
    }
    return depth.get();
  }

  protected int decrementNestedDepth(Object depthTrackingKey) {
    Counter depth = (Counter) nestedDepth.get(depthTrackingKey);
    if (depth == null) { return 0; }

    depth.decrement();
    return depth.get();
  }

  protected static class LockStatConfig {
    private final static int DEFAULT_GATHER_INTERVAL = 1;
    private final static int DEFAULT_TRACE_DEPTH     = 0;

    private int              traceDepth              = DEFAULT_TRACE_DEPTH;
    // Currently, the gatherInterval data is not being honored.
    private int              gatherInterval          = DEFAULT_GATHER_INTERVAL;

    public LockStatConfig() {
      reset();
    }

    public LockStatConfig(int traceDepth, int gatherInterval) {
      this.traceDepth = traceDepth;
      this.gatherInterval = gatherInterval;
    }

    public int getGatherInterval() {
      return gatherInterval;
    }

    public int getTraceDepth() {
      return traceDepth;
    }

    public void setTraceDepth(int traceDepth) {
      this.traceDepth = traceDepth;
    }

    public void setGatherInterval(int gatherInterval) {
      this.gatherInterval = gatherInterval;
    }

    public void setConfig(int traceDepth, int gatherInterval) {
      this.traceDepth = traceDepth;
      this.gatherInterval = gatherInterval;
    }

    public void reset() {
      this.traceDepth = DEFAULT_TRACE_DEPTH;
      this.gatherInterval = DEFAULT_GATHER_INTERVAL;
      TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("l1.lock.statistics");
      if (tcProperties != null) {
        this.traceDepth = tcProperties.getInt("traceDepth", DEFAULT_TRACE_DEPTH);
        this.gatherInterval = tcProperties.getInt("gatherInterval", DEFAULT_GATHER_INTERVAL);
      }
    }
  }

}
