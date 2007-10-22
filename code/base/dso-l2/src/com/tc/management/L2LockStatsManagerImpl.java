/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.async.api.Sink;
import com.tc.management.stats.LRUMap;
import com.tc.management.stats.TopN;
import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.impl.TCStackTraceElement;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.LockHolder;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class L2LockStatsManagerImpl implements L2LockStatsManager {
  private final static int                                     TOP_N                                    = 100;
  private final static Comparator                              LOCK_REQUESTED_COMPARATOR                = new Comparator() {
                                                                                                          public int compare(
                                                                                                                             Object o1,
                                                                                                                             Object o2) {
                                                                                                            LockStat s1 = (LockStat) o1;
                                                                                                            LockStat s2 = (LockStat) o2;
                                                                                                            if (s1 == s2) { return 0; }
                                                                                                            if (s1
                                                                                                                .getNumOfLockRequested() <= s2
                                                                                                                .getNumOfLockRequested()) { return -1; }
                                                                                                            return 1;
                                                                                                          }
                                                                                                        };
  private final static Comparator                              LOCK_HOP_REQUESTED_COMPARATOR            = new Comparator() {
                                                                                                          public int compare(
                                                                                                                             Object o1,
                                                                                                                             Object o2) {
                                                                                                            LockStat s1 = (LockStat) o1;
                                                                                                            LockStat s2 = (LockStat) o2;
                                                                                                            if (s1 == s2) { return 0; }
                                                                                                            if (s1
                                                                                                                .getNumOfLockHopRequests() <= s2
                                                                                                                .getNumOfLockHopRequests()) { return -1; }
                                                                                                            return 1;
                                                                                                          }
                                                                                                        };
  private final static Comparator                              LOCK_HELD_COMPARATOR                     = new Comparator() {
                                                                                                          public int compare(
                                                                                                                             Object o1,
                                                                                                                             Object o2) {
                                                                                                            LockHolder s1 = (LockHolder) o1;
                                                                                                            LockHolder s2 = (LockHolder) o2;
                                                                                                            if (s1 == s2) { return 0; }
                                                                                                            if (s1
                                                                                                                .getHeldTimeInMillis() <= s2
                                                                                                                .getHeldTimeInMillis()) { return -1; }
                                                                                                            return 1;
                                                                                                          }
                                                                                                        };
  private final static Comparator                              PENDING_LOCK_REQUESTS_COMPARATOR         = new Comparator() {
                                                                                                          public int compare(
                                                                                                                             Object o1,
                                                                                                                             Object o2) {
                                                                                                            LockStat s1 = (LockStat) o1;
                                                                                                            LockStat s2 = (LockStat) o2;
                                                                                                            if (s1 == s2) { return 0; }
                                                                                                            if (s1
                                                                                                                .getNumOfPendingRequests() <= s2
                                                                                                                .getNumOfPendingRequests()) { return -1; }
                                                                                                            return 1;
                                                                                                          }
                                                                                                        };
  private final static Comparator                              LOCK_ACQUIRED_WAITING_COMPARATOR         = new Comparator() {
                                                                                                          public int compare(
                                                                                                                             Object o1,
                                                                                                                             Object o2) {
                                                                                                            LockHolder s1 = (LockHolder) o1;
                                                                                                            LockHolder s2 = (LockHolder) o2;
                                                                                                            if (s1 == s2) { return 0; }
                                                                                                            if (s1
                                                                                                                .getWaitTimeInMillis() <= s2
                                                                                                                .getWaitTimeInMillis()) { return -1; }
                                                                                                            return 1;
                                                                                                          }
                                                                                                        };
  private final static Comparator                              AVERAGE_LOCK_HELD_COMPARATOR             = new Comparator() {
                                                                                                          public int compare(
                                                                                                                             Object o1,
                                                                                                                             Object o2) {
                                                                                                            LockStat s1 = (LockStat) o1;
                                                                                                            LockStat s2 = (LockStat) o2;
                                                                                                            if (s1 == s2) { return 0; }
                                                                                                            if (s1
                                                                                                                .getAvgHeldTimeInMillis() <= s2
                                                                                                                .getAvgHeldTimeInMillis()) { return -1; }
                                                                                                            return 1;
                                                                                                          }
                                                                                                        };
  private final static Comparator                              AVERAGE_LOCK_ACQUIRED_WAITING_COMPARATOR = new Comparator() {
                                                                                                          public int compare(
                                                                                                                             Object o1,
                                                                                                                             Object o2) {
                                                                                                            LockStat s1 = (LockStat) o1;
                                                                                                            LockStat s2 = (LockStat) o2;
                                                                                                            if (s1 == s2) { return 0; }
                                                                                                            if (s1
                                                                                                                .getAvgWaitTimeInMillis() <= s2
                                                                                                                .getAvgWaitTimeInMillis()) { return -1; }
                                                                                                            return 1;
                                                                                                          }
                                                                                                        };

  private final LockHolderStats                                holderStats;
  private final Map<LockID, LockStat>                          lockStats;
  private DSOChannelManager                                    channelManager;
  private LockManager                                          lockManager;
  private Sink                                                 sink;
  private final int                                            topN;
  private final Map<LockID, ClientLockStatContext>             clientStatEnabledLock;
  private final Map<LockID, Map<LockKey, LockStackTracesStat>> lockStackTraces;
  private boolean                                              lockStatEnabled;

  public L2LockStatsManagerImpl() {
    TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("lock.statistics");
    if (tcProperties == null) {
      this.lockStatEnabled = false;
      this.topN = TOP_N;
    } else {
      if (tcProperties.getProperty("enabled") == null) {
        this.lockStatEnabled = false;
      } else {
        this.lockStatEnabled = tcProperties.getBoolean("enabled");
      }
      this.topN = tcProperties.getInt("max", TOP_N);
    }
    this.clientStatEnabledLock = new HashMap<LockID, ClientLockStatContext>();
    this.holderStats = new LockHolderStats(topN);
    this.lockStats = new LRUMap(topN);
    this.lockStackTraces = new LRUMap(topN);
  }

  private void clearAllStatistics() {
    this.clientStatEnabledLock.clear();
    this.holderStats.clear();
    this.lockStats.clear();
    this.lockStackTraces.clear();
  }

  public synchronized void start(DSOChannelManager channelManager, LockManager lockManager, Sink sink) {
    this.channelManager = channelManager;
    this.lockManager = lockManager;
    this.sink = sink;
  }

  public synchronized void enableLockStatistics() {
    this.lockStatEnabled = true;
  }

  public synchronized void disableLockStatistics() {
    this.lockStatEnabled = false;
    for (Iterator i = clientStatEnabledLock.keySet().iterator(); i.hasNext();) {
      LockID lockID = (LockID) i.next();
      disableClientStackTrace(lockID);
    }
    clearAllStatistics();
  }

  private LockHolder newLockHolder(LockID lockID, NodeID nodeID, ThreadID threadID, int lockLevel, long timeStamp) {
    return new LockHolder(lockID, nodeID, channelManager.getChannelAddress(nodeID), threadID, lockLevel, timeStamp);
  }

  private LockKey newLockKey(LockID lockID, NodeID nodeID, ThreadID threadID) {
    return new LockKey(lockID, nodeID, threadID);
  }

  public void enableClientStackTrace(LockID lockID) {
    ClientLockStatContext clientLockStatContext = new ClientLockStatContext();
    enableClientStat(lockID, clientLockStatContext);
  }

  public void enableClientStackTrace(LockID lockID, int stackTraceDepth, int statCollectFrequency) {
    ClientLockStatContext clientLockStatContext = new ClientLockStatContext(statCollectFrequency, stackTraceDepth);
    enableClientStat(lockID, clientLockStatContext);
  }

  private void enableClientStat(LockID lockID, ClientLockStatContext clientLockStatContext) {
    if (clientLockStatContext.getStackTraceDepth() == 0) {
      disableClientStackTrace(lockID);
      return;
    }
    
    synchronized (this) {
      if (!lockStatEnabled) { return; }
      
      lockStackTraces.remove(lockID);
      clientStatEnabledLock.put(lockID, clientLockStatContext);
    }
    lockManager.enableClientStat(lockID, sink, clientLockStatContext.getStackTraceDepth(), clientLockStatContext
        .getCollectFrequency());
  }

  public void disableClientStackTrace(LockID lockID) {
    Set statEnabledClients = null;
    synchronized (this) {
      if (!lockStatEnabled) { return; }
      
      lockStackTraces.remove(lockID);
      ClientLockStatContext clientLockStatContext = clientStatEnabledLock.remove(lockID);
      statEnabledClients = clientLockStatContext.getStatEnabledClients();
    }
    if (statEnabledClients != null) {
      lockManager.disableClientStat(lockID, statEnabledClients, sink);
    }
  }

  public synchronized boolean isClientLockStackTraceEnable(LockID lockID) {
    return clientStatEnabledLock.containsKey(lockID);
  }

  public synchronized int getLockStackTraceDepth(LockID lockID) {
    ClientLockStatContext clientLockStatContext = clientStatEnabledLock.get(lockID);
    return clientLockStatContext.getStackTraceDepth();
  }

  public synchronized int getLockStatCollectFrequency(LockID lockID) {
    ClientLockStatContext clientLockStatContext = clientStatEnabledLock.get(lockID);
    return clientLockStatContext.getCollectFrequency();
  }

  public synchronized boolean isLockStackTraceEnabledInClient(LockID lockID, NodeID nodeID) {
    ClientLockStatContext clientLockStatContext = clientStatEnabledLock.get(lockID);
    if (clientLockStatContext == null) { return false; }
    return clientLockStatContext.isClientLockStatEnabled(nodeID);
  }

  public synchronized void recordClientStackTraceEnabled(LockID lockID, NodeID nodeID) {
    if (!lockStatEnabled) { return; }

    ClientLockStatContext clientLockStatContext = clientStatEnabledLock.get(lockID);
    Assert.assertNotNull(clientLockStatContext);
    clientLockStatContext.addClient(nodeID);
  }
  
  public synchronized void lockHopped(LockID lockID) {
    if (!lockStatEnabled) { return; }
    
    LockStat lockStat = lockStats.get(lockID);
    if (lockStat != null) {
      lockStat.lockHop();
    }
  }

  public synchronized void lockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, int lockLevel) {
    if (!lockStatEnabled) { return; }

    LockStat lockStat = lockStats.get(lockID);
    if (lockStat == null) {
      lockStat = new LockStat(lockID);
      lockStats.put(lockID, lockStat);
    }
    lockStat.lockRequested();

    LockHolder lockHolder = newLockHolder(lockID, nodeID, threadID, lockLevel, System.currentTimeMillis());
    addLockHolder(newLockKey(lockID, nodeID, threadID), lockHolder);
  }

  private LockHolder getLockHolder(LockKey key) {
    return (LockHolder) holderStats.get(key);
  }

  public void addLockHolder(LockKey key, LockHolder lockHolder) {
    holderStats.put(key, lockHolder);
  }

  public synchronized void lockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy,
                                       long lockAwardTimestamp) {
    if (!lockStatEnabled) { return; }

    LockKey lockKey = newLockKey(lockID, nodeID, threadID);
    LockHolder lockHolder = getLockHolder(lockKey);

    if (lockHolder != null) { // a lock holder could be null if jmx is enabled during runtime
      lockHolder.lockAcquired(lockAwardTimestamp);
      if (isGreedy) {
        holderStats.remove(lockKey, lockHolder);
        lockKey = newLockKey(lockID, nodeID, ThreadID.VM_ID);
        holderStats.put(lockKey, lockHolder);
      }
    }

    LockStat lockStat = lockStats.get(lockID);
    if (lockStat != null) {
      lockStat.lockAwarded();

      if (lockHolder != null) {
        lockStat.aggregateWaitTime(lockHolder.getAndSetWaitTimeInMillis());
      }
    }
  }

  public synchronized void lockReleased(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatEnabled) { return; }

    LockStat lockStat = lockStats.get(lockID);

    LockHolder lockHolder = lockReleasedInternal(lockID, nodeID, threadID);
    if (lockHolder == null) { return; }

    if (lockStat != null) {
      lockStat.lockReleased();
      lockStat.aggregateHeldTime(lockHolder.getAndSetHeldTimeInMillis());
    }
  }

  private LockHolder lockReleasedInternal(LockID lockID, NodeID nodeID, ThreadID threadID) {
    LockKey lockKey = newLockKey(lockID, nodeID, threadID);
    LockHolder lockHolder = getLockHolder(lockKey);
    if (lockHolder == null) { return null; }

    lockHolder.lockReleased();
    holderStats.moveToHistory(lockKey, lockHolder);

    return lockHolder;
  }

  public synchronized void lockRejected(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatEnabled) { return; }

    LockStat lockStat = lockStats.get(lockID);
    if (lockStat != null) {
      lockStat.lockRejected();
    }

    lockReleasedInternal(lockID, nodeID, threadID);
  }

  public synchronized void lockWait(LockID lockID) {
    if (!lockStatEnabled) { return; }

    LockStat lockStat = lockStats.get(lockID);
    if (lockStat != null) {
      lockStat.lockWaited();
    }
  }

  public synchronized void lockNotified(LockID lockID, int n) {
    if (!lockStatEnabled) { return; }

    LockStat lockStat = lockStats.get(lockID);
    if (lockStat != null) {
      lockStat.lockNotified(n);
    }
  }

  public synchronized void recordStackTraces(LockID lockID, NodeID nodeID, List stackTraces) {
    if (!lockStatEnabled) { return; }

    Map<LockKey, LockStackTracesStat> existingStackTraces = lockStackTraces.get(lockID);
    LockKey lockKey = new LockKey(lockID, nodeID);
    if (existingStackTraces == null) {
      existingStackTraces = new LRUMap(topN);
      existingStackTraces.put(lockKey, new LockStackTracesStat(nodeID, lockID, stackTraces, topN));
      lockStackTraces.put(lockID, existingStackTraces);
    } else {
      LockStackTracesStat stackTracesStat = existingStackTraces.get(lockKey);
      if (stackTracesStat == null) {
        stackTracesStat = new LockStackTracesStat(nodeID, lockID, stackTraces, topN);
        existingStackTraces.put(lockKey, stackTracesStat);
      } else {
        stackTracesStat.addStackTraces(stackTraces);
      }
    }
  }

  public synchronized long getNumberOfLockRequested(LockID lockID) {
    if (!lockStatEnabled) { return 0; }

    return lockStats.get(lockID).getNumOfLockRequested();
  }

  public synchronized long getNumberOfLockReleased(LockID lockID) {
    if (!lockStatEnabled) { return 0; }

    return lockStats.get(lockID).getNumOfLockReleased();
  }

  public synchronized long getNumberOfPendingRequests(LockID lockID) {
    if (!lockStatEnabled) { return 0; }

    return lockStats.get(lockID).getNumOfPendingRequests();
  }

  public synchronized LockHolder getLockHolder(LockID lockID, NodeID nodeID, ThreadID threadID) {
    if (!lockStatEnabled) { return null; }

    return getLockHolder(newLockKey(lockID, nodeID, threadID));
  }

  public synchronized long getNumberOfLockHopRequests(LockID lockID) {
    if (!lockStatEnabled) { return 0; }

    return lockStats.get(lockID).getNumOfLockHopRequests();
  }

  public synchronized Collection getTopLockStats(int n) {
    if (!lockStatEnabled) { return Collections.EMPTY_LIST; }

    Collection allLockStats = lockStats.values();
    TopN topNLockStats = new TopN(LOCK_REQUESTED_COMPARATOR, n);
    topNLockStats.evaluate(allLockStats);
    return topNLockStats.getDataSnapshot();
  }

  public synchronized Collection getTopAggregateLockHolderStats(int n) {
    if (!lockStatEnabled) { return Collections.EMPTY_LIST; }

    return holderStats.aggregateTopN(lockStats, n, AVERAGE_LOCK_HELD_COMPARATOR);
  }

  public synchronized Collection getTopLockHoldersStats(int n) {
    if (!lockStatEnabled) { return Collections.EMPTY_LIST; }

    return holderStats.topN(n, LOCK_HELD_COMPARATOR);
  }

  public synchronized Collection getTopAggregateWaitingLocks(int n) {
    if (!lockStatEnabled) { return Collections.EMPTY_LIST; }

    return holderStats.aggregateTopN(lockStats, n, AVERAGE_LOCK_ACQUIRED_WAITING_COMPARATOR);
  }

  public synchronized Collection getTopWaitingLocks(int n) {
    if (!lockStatEnabled) { return Collections.EMPTY_LIST; }

    return holderStats.topN(n, LOCK_ACQUIRED_WAITING_COMPARATOR);
  }

  public synchronized Collection getTopContendedLocks(int n) {
    if (!lockStatEnabled) { return Collections.EMPTY_LIST; }

    Collection allLockStats = lockStats.values();
    TopN topNLockStats = new TopN(PENDING_LOCK_REQUESTS_COMPARATOR, n);
    topNLockStats.evaluate(allLockStats);
    return topNLockStats.getDataSnapshot();
  }

  public synchronized Collection getTopLockHops(int n) {
    if (!lockStatEnabled) { return Collections.EMPTY_LIST; }

    Collection allLockStats = lockStats.values();
    TopN topNLockStats = new TopN(LOCK_HOP_REQUESTED_COMPARATOR, n);
    topNLockStats.evaluate(allLockStats);
    return topNLockStats.getDataSnapshot();
  }

  public synchronized Collection getStackTraces(LockID lockID) {
    if (!lockStatEnabled) { return Collections.EMPTY_LIST; }

    Map<?, LockStackTracesStat> stackTraces = lockStackTraces.get(lockID);
    if (stackTraces == null) { return Collections.EMPTY_LIST; }
    return new ArrayList<LockStackTracesStat>(stackTraces.values());
  }
  
  public synchronized void clearAllStatsFor(NodeID nodeID) {
    if (! lockStatEnabled) { return; }
    
    this.holderStats.clearAllStatsFor(nodeID);
  }
  
  private static class LockKey {
    private LockID   lockID;
    private NodeID   nodeID;
    private ThreadID threadID;
    private int      hashCode;

    private LockKey  subKey;

    public LockKey(LockID lockID, NodeID nodeID) {
      this.lockID = lockID;
      this.nodeID = nodeID;
      this.threadID = null;
      this.subKey = null;
      this.hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(nodeID).toHashCode();
    }

    public LockKey(LockID lockID, NodeID nodeID, ThreadID threadID) {
      this.lockID = lockID;
      this.nodeID = nodeID;
      this.threadID = threadID;
      this.hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(nodeID).append(threadID).toHashCode();
      this.subKey = new LockKey(lockID, nodeID);
    }

    public String toString() {
      return "LockKey [ " + lockID + ", " + nodeID + ", " + threadID + ", " + hashCode + "] ";
    }

    public NodeID getNodeID() {
      return nodeID;
    }

    public LockID getLockID() {
      return lockID;
    }

    public ThreadID getThreadID() {
      return threadID;
    }

    public LockKey subKey() {
      return subKey;
    }

    public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof LockKey)) return false;
      LockKey cmp = (LockKey) o;
      if (threadID != null) {
        return lockID.equals(cmp.lockID) && nodeID.equals(cmp.nodeID) && threadID.equals(cmp.threadID);
      } else {
        return lockID.equals(cmp.lockID) && nodeID.equals(cmp.nodeID);
      }
    }

    public int hashCode() {
      return hashCode;
    }
  }

  private static class LockHolderStats {
    private static class PendingStat {
      private long numOfHolders;
      private long totalWaitTimeInMillis;
      private long totalHeldTimeInMillis;

      public PendingStat(long waitTimeInMillis, long heldTimeInMillis) {
        addPendingHolderData(waitTimeInMillis, heldTimeInMillis);
      }

      public void addPendingHolderData(long waitTimeInMillis, long heldTimeInMillis) {
        this.numOfHolders++;
        this.totalHeldTimeInMillis += heldTimeInMillis;
        this.totalWaitTimeInMillis += waitTimeInMillis;
      }
    }

    private final static int                             NO_LIMIT = -1;

    private final Map<LockKey, Map<LockKey, LockHolder>> pendingData;  // map<LockKey.subKey, map<LockKey, LockHolder>>
    private final LinkedList<LockHolder>                 historyData;  // list of LockHolder
    private final int                                    maxSize;

    public LockHolderStats() {
      this(NO_LIMIT);
    }

    public LockHolderStats(int maxSize) {
      pendingData = new HashMap<LockKey, Map<LockKey, LockHolder>>();
      historyData = new LinkedList<LockHolder>();
      this.maxSize = maxSize;
    }

    public void clear() {
      this.pendingData.clear();
      this.historyData.clear();
    }

    public void put(LockKey key, LockHolder value) {
      LockKey subKey = key.subKey();
      Map<LockKey, LockHolder> lockHolders = pendingData.get(subKey);
      if (lockHolders == null) {
        lockHolders = new HashMap<LockKey, LockHolder>();
        pendingData.put(subKey, lockHolders);
      }
      lockHolders.put(key, value);
    }

    public void remove(LockKey key, Object value) {
      LockKey subKey = key.subKey();
      Map lockHolders = pendingData.get(subKey);
      lockHolders.remove(key);
    }

    public void moveToHistory(LockKey key, Object value) {
      LockKey subKey = key.subKey();
      Map lockHolders = pendingData.get(subKey);
      LockHolder o = (LockHolder) lockHolders.remove(key);
      historyData.addLast(o);
      removeOldDataIfNeeded();
    }

    private void removeOldDataIfNeeded() {
      if (maxSize != NO_LIMIT && historyData.size() > maxSize) {
        historyData.removeFirst();
      }
    }

    public Object get(LockKey key) {
      LockKey subKey = key.subKey();
      Map lockHolders = pendingData.get(subKey);
      if (lockHolders == null) return null;
      if (lockHolders.size() == 0) return null;

      return lockHolders.get(key);
    }

    public boolean contains(LockKey key) {
      LockKey subKey = key.subKey();
      Map lockHolders = pendingData.get(subKey);
      return lockHolders.containsKey(key);
    }

    public Collection aggregateTopN(Map lockStats, int n, Comparator comparator) {
      Map<LockID, PendingStat> aggregateData = new HashMap<LockID, PendingStat>(); // map<LockID, PendingStat>

      Collection val = pendingData.values();
      for (Iterator i = val.iterator(); i.hasNext();) {
        Map lockHolders = (Map) i.next();
        for (Iterator j = lockHolders.values().iterator(); j.hasNext();) {
          LockHolder lockHolder = (LockHolder) j.next();
          updateAggregateLockHolder(aggregateData, lockHolder);
        }
      }
      for (Iterator i = aggregateData.keySet().iterator(); i.hasNext();) {
        LockID lockID = (LockID) i.next();
        PendingStat pendingStat = aggregateData.get(lockID);
        LockStat lockStat = (LockStat) lockStats.get(lockID);
        lockStat.aggregateAvgWaitTimeInMillis(pendingStat.totalWaitTimeInMillis, pendingStat.numOfHolders);
        lockStat.aggregateAvgHeldTimeInMillis(pendingStat.totalHeldTimeInMillis, pendingStat.numOfHolders);
      }
      TopN topN = new TopN(comparator, n);
      topN.evaluate(lockStats.values());
      return topN.getDataSnapshot();
    }

    private void updateAggregateLockHolder(Map<LockID, PendingStat> aggregateData, LockHolder lockHolder) {
      PendingStat pendingStat = aggregateData.get(lockHolder.getLockID());
      if (pendingStat == null) {
        pendingStat = new PendingStat(lockHolder.getAndSetWaitTimeInMillis(), lockHolder.getAndSetHeldTimeInMillis());
        aggregateData.put(lockHolder.getLockID(), pendingStat);
      } else {
        pendingStat
            .addPendingHolderData(lockHolder.getAndSetWaitTimeInMillis(), lockHolder.getAndSetHeldTimeInMillis());
      }
    }

    public Collection topN(int n, Comparator comparator) {
      Collection val = pendingData.values();

      TopN topN = new TopN(comparator, n);
      for (Iterator i = val.iterator(); i.hasNext();) {
        Map lockHolders = (Map) i.next();
        for (Iterator j = lockHolders.values().iterator(); j.hasNext();) {
          LockHolder lockHolder = (LockHolder) j.next();
          lockHolder.getAndSetHeldTimeInMillis();
          lockHolder.getAndSetWaitTimeInMillis();
          topN.evaluate(lockHolder);
        }
      }
      topN.evaluate(historyData);
      return topN.getDataSnapshot();
    }
    
    public void clearAllStatsFor(NodeID nodeID) {
      Set<LockKey> lockKeys = pendingData.keySet();
      for (Iterator<LockKey> i=lockKeys.iterator(); i.hasNext(); ) {
        LockKey key = i.next();
        if (nodeID.equals(key.getNodeID())) {
          i.remove();
        }
      }
    }

    public String toString() {
      return pendingData.toString();
    }
  }

  public static class LockStackTracesStat implements Serializable {
    private final NodeID     nodeID;
    private final LockID     lockID;
    private final LinkedList<TCStackTraceElement> stackTraces;
    private final int        maxNumOfStackTraces;

    public LockStackTracesStat(NodeID nodeID, LockID lockID, List newStackTraces, int maxNumOfStackTraces) {
      this.nodeID = nodeID;
      this.lockID = lockID;
      this.stackTraces = new LinkedList<TCStackTraceElement>();
      this.maxNumOfStackTraces = maxNumOfStackTraces;
      addStackTraces(newStackTraces);
    }

    public void addStackTraces(List newStackTraces) {
      for (Iterator i = newStackTraces.iterator(); i.hasNext();) {
        this.stackTraces.addFirst((TCStackTraceElement)i.next());
      }
      removeIfOverFlow();
    }

    private void removeIfOverFlow() {
      while (this.stackTraces.size() > maxNumOfStackTraces) {
        this.stackTraces.removeLast();
      }
    }

    public NodeID getNodeID() {
      return this.nodeID;
    }

    public List getStackTraces() {
      return this.stackTraces;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer(nodeID.toString());
      sb.append(" ");
      sb.append(lockID);
      sb.append("\n");
      for (Iterator i = stackTraces.iterator(); i.hasNext();) {
        sb.append(i.next().toString());
        sb.append("\n\n");
      }
      return sb.toString();
    }
  }

  public static class LockStat implements Serializable {
    private final static long NON_SET_TIME_MILLIS = -1;
    private static final long serialVersionUID    = 618840956490853662L;

    private final LockID      lockID;
    private long              numOfPendingRequests;
    private long              numOfPendingWaiters;
    private long              numOfRequested;
    private long              numOfReleased;
    private long              numOfRejected;
    private long              numOfLockHopRequests;
    private long              numOfAwarded;
    private long              totalWaitTimeInMillis;
    private long              totalHeldTimeInMillis;
    private long              avgWaitTimeInMillis;
    private long              avgHeldTimeInMillis;

    public LockStat(LockID lockID) {
      this.lockID = lockID;
      numOfRequested = 0;
      numOfReleased = 0;
      numOfPendingRequests = 0;
      numOfPendingWaiters = 0;
      numOfLockHopRequests = 0;
      numOfAwarded = 0;
      totalWaitTimeInMillis = 0;
      totalHeldTimeInMillis = 0;
      avgWaitTimeInMillis = NON_SET_TIME_MILLIS;
      avgHeldTimeInMillis = NON_SET_TIME_MILLIS;
    }

    public LockID getLockID() {
      return lockID;
    }

    public void lockRequested() {
      numOfRequested++;
      numOfPendingRequests++;
    }

    public void lockAwarded() {
      numOfPendingRequests--;
      numOfAwarded++;
    }

    public void lockRejected() {
      numOfPendingRequests--;
      numOfRejected++;
    }

    public void lockWaited() {
      numOfPendingWaiters++;
    }

    public void lockNotified(int n) {
      numOfPendingWaiters -= n;
    }

    public void lockHop() {
      numOfLockHopRequests++;
    }

    public long getNumOfLockRequested() {
      return numOfRequested;
    }

    public void lockReleased() {
      numOfReleased++;
    }

    public long getNumOfLockReleased() {
      return numOfReleased;
    }

    public long getNumOfPendingRequests() {
      return numOfPendingRequests;
    }

    public long getNumOfPendingWaiters() {
      return numOfPendingWaiters;
    }

    public long getNumOfLockHopRequests() {
      return numOfLockHopRequests;
    }

    public void aggregateWaitTime(long waitTimeInMillis) {
      this.totalWaitTimeInMillis += waitTimeInMillis;
    }

    public void aggregateHeldTime(long heldTimeInMillis) {
      this.totalHeldTimeInMillis += heldTimeInMillis;
    }

    public long getAvgWaitTimeInMillis() {
      if (avgWaitTimeInMillis == NON_SET_TIME_MILLIS) {
        aggregateAvgWaitTimeInMillis(0, 0);
      }
      return avgWaitTimeInMillis;
    }

    public long getAvgHeldTimeInMillis() {
      if (avgHeldTimeInMillis == NON_SET_TIME_MILLIS) {
        aggregateAvgHeldTimeInMillis(0, 0);
      }
      return avgHeldTimeInMillis;
    }

    public void aggregateAvgHeldTimeInMillis(long totalHeldTimeInMillis, long numOfReleased) {
      avgHeldTimeInMillis = NON_SET_TIME_MILLIS;
      numOfReleased += this.numOfReleased;
      totalHeldTimeInMillis += this.totalHeldTimeInMillis;
      if (numOfReleased > 0) {
        avgHeldTimeInMillis = totalHeldTimeInMillis / numOfReleased;
      }
    }

    public void aggregateAvgWaitTimeInMillis(long totalWaitTimeInMillis, long numOfAwarded) {
      avgWaitTimeInMillis = NON_SET_TIME_MILLIS;
      numOfAwarded += this.numOfAwarded;
      totalWaitTimeInMillis += this.totalWaitTimeInMillis;
      if (numOfAwarded > 0) {
        avgWaitTimeInMillis = totalWaitTimeInMillis / numOfAwarded;
      }
    }

    public String toString() {
      StringBuffer sb = new StringBuffer("[LockID: ");
      sb.append(lockID);
      sb.append(", number of requested: ");
      sb.append(numOfRequested);
      sb.append(", number of released: ");
      sb.append(numOfReleased);
      sb.append(", number of pending requests: ");
      sb.append(numOfPendingRequests);
      sb.append(", number of pending waiters: ");
      sb.append(numOfPendingWaiters);
      sb.append(", number of lock hop: ");
      sb.append(numOfLockHopRequests);
      sb.append("]");
      return sb.toString();
    }
  }

}
