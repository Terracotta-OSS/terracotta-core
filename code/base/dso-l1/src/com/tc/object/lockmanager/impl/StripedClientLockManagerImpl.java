/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TextDecoratorTCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.net.NodeID;
import com.tc.net.OrderedGroupIDs;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.ClientLockManagerConfig;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.TCLockTimer;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.TimerSpec;
import com.tc.text.PrettyPrinter;
import com.tc.util.runtime.LockInfoByThreadID;

public class StripedClientLockManagerImpl implements ClientLockManager {

  private final ClientLockManagerImpl lockManagers[];
  private final int                   segmentShift;
  private final int                   segmentMask;
  private final TCLogger              logger;

  public StripedClientLockManagerImpl(final LockDistributionStrategy strategy, final OrderedGroupIDs groupIds,
                                      final TCLogger logger, final RemoteLockManager remoteLockManager,
                                      final SessionManager sessionManager, final ClientLockStatManager lockStatManager,
                                      final ClientLockManagerConfig clientLockManagerConfig) {
    this.logger = logger;
    int stripedCount = clientLockManagerConfig.getStripedCount();

    int sshift = 0;
    int ssize = 1;
    while (ssize < stripedCount) {
      ++sshift;
      ssize <<= 1;
    }
    this.segmentShift = 32 - sshift;
    this.segmentMask = ssize - 1;

    this.lockManagers = new ClientLockManagerImpl[ssize];
    TCLockTimer waitTimer = new TCLockTimerImpl();
    for (int i = 0; i < this.lockManagers.length; i++) {
      this.lockManagers[i] = new ClientLockManagerImpl(strategy, groupIds, new TextDecoratorTCLogger(logger, "LM[" + i
                                                                                                             + "]"),
                                                       remoteLockManager, sessionManager, lockStatManager,
                                                       clientLockManagerConfig, waitTimer);
    }
  }

  /**
   * Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions.
   */
  private static int hash(int h) {
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  /**
   * Returns the segment that should be used for key with given hash
   * 
   * @param hash the hash code for the key
   * @return the segment
   */
  final ClientLockManagerImpl lockManagerFor(final LockID lock) {
    return lockManagerFor(lock.asString());
  }

  private ClientLockManagerImpl lockManagerFor(final String lockID) {
    int hash = hash(lockID.hashCode());
    return this.lockManagers[(hash >>> this.segmentShift) & this.segmentMask];
  }

  public synchronized void pause(final NodeID remote, final int disconnected) {
    for (ClientLockManagerImpl lockManager : this.lockManagers) {
      lockManager.pause(remote, disconnected);
    }
  }

  public synchronized void unpause(final NodeID remote, final int disconnected) {
    for (ClientLockManagerImpl lockManager : this.lockManagers) {
      lockManager.unpause(remote, disconnected);
    }
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    for (ClientLockManagerImpl lockManager : this.lockManagers) {
      lockManager.initializeHandshake(thisNode, remoteNode, handshakeMessage);
    }
  }

  public void lock(final LockID id, final ThreadID threadID, final int lockType, final String lockObjectType,
                   final String contextInfo) {
    lockManagerFor(id).lock(id, threadID, lockType, lockObjectType, contextInfo);
  }

  public void awardLock(final NodeID nid, final SessionID sessionID, final LockID id, final ThreadID threadID,
                        final int type) {
    lockManagerFor(id).awardLock(nid, sessionID, id, threadID, type);
  }

  public void cannotAwardLock(final NodeID nid, final SessionID sessionID, final LockID id, final ThreadID threadID,
                              final int type) {
    lockManagerFor(id).cannotAwardLock(nid, sessionID, id, threadID, type);
  }

  public boolean isLocked(final LockID lockID, final ThreadID threadID, final int lockLevel) {
    return lockManagerFor(lockID).isLocked(lockID, threadID, lockLevel);
  }

  public int localHeldCount(final LockID lockID, final int lockLevel, final ThreadID threadID) {
    return lockManagerFor(lockID).localHeldCount(lockID, lockLevel, threadID);
  }

  public LockID lockIDFor(final String id) {
    return lockManagerFor(id).lockIDFor(id);
  }

  public void notified(final LockID lockID, final ThreadID threadID) {
    lockManagerFor(lockID).notified(lockID, threadID);
  }

  public Notify notify(final LockID lockID, final ThreadID threadID, final boolean all) {
    return lockManagerFor(lockID).notify(lockID, threadID, all);
  }

  public int queueLength(final LockID lockID, final ThreadID threadID) {
    return lockManagerFor(lockID).queueLength(lockID, threadID);
  }

  public void recall(final LockID lockID, final ThreadID threadID, final int level, final int leaseTimeInMs) {
    lockManagerFor(lockID).recall(lockID, threadID, level, leaseTimeInMs);
  }

  public void lockInterruptibly(final LockID id, final ThreadID threadID, final int lockType,
                                final String lockObjectType, final String contextInfo) throws InterruptedException {
    lockManagerFor(id).lockInterruptibly(id, threadID, lockType, lockObjectType, contextInfo);
  }

  public boolean tryLock(final LockID id, final ThreadID threadID, final TimerSpec timeout, final int lockType,
                         final String lockObjectType) {
    return lockManagerFor(id).tryLock(id, threadID, timeout, lockType, lockObjectType);
  }

  public void unlock(final LockID id, final ThreadID threadID) {
    lockManagerFor(id).unlock(id, threadID);
  }

  public void wait(final LockID lockID, final ThreadID threadID, final TimerSpec call, final Object waitObject,
                   final WaitListener listener) throws InterruptedException {
    lockManagerFor(lockID).wait(lockID, threadID, call, waitObject, listener);
  }

  public int waitLength(final LockID lockID, final ThreadID threadID) {
    return lockManagerFor(lockID).waitLength(lockID, threadID);
  }

  public void waitTimedOut(final LockID lockID, final ThreadID threadID) {
    lockManagerFor(lockID).waitTimedOut(lockID, threadID);
  }

  public void pinLock(final LockID lockId) {
    lockManagerFor(lockId).pinLock(lockId);
  }

  public void unpinLock(final LockID lockId) {
    lockManagerFor(lockId).unpinLock(lockId);
  }

  public void evictLock(final LockID lockId) {
    lockManagerFor(lockId).evictLock(lockId);
  }

  public void addAllLocksTo(final LockInfoByThreadID lockInfo) {
    for (ClientLockManagerImpl lockManager : this.lockManagers) {
      lockManager.addAllLocksTo(lockInfo);
    }
  }

  public void queryLockCommit(final ThreadID threadID, final GlobalLockInfo globalLockInfo) {
    lockManagerFor(globalLockInfo.getLockID()).queryLockCommit(threadID, globalLockInfo);
  }

  public void requestLockSpecs(NodeID nodeID) {
    for (ClientLockManagerImpl lockManager : this.lockManagers) {
      lockManager.requestLockSpecs(nodeID);
    }
  }

  public void setLockStatisticsConfig(final int traceDepth, final int gatherInterval) {
    for (ClientLockManagerImpl lockManager : this.lockManagers) {
      lockManager.setLockStatisticsConfig(traceDepth, gatherInterval);
    }
  }

  public void setLockStatisticsEnabled(final boolean statEnable) {
    for (ClientLockManagerImpl lockManager : this.lockManagers) {
      lockManager.setLockStatisticsEnabled(statEnable);
    }
  }

  public String dump() {
    StringBuffer sb = new StringBuffer("StripedClientLockManagerImpl : { \n");
    for (int i = 0; i < this.lockManagers.length; i++) {
      sb.append('[').append(i).append("] = ");
      sb.append(this.lockManagers[i].dump()).append("\n");
    }
    sb.append("}");
    return sb.toString();
  }

  public void dumpToLogger() {
    this.logger.info(dump());
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.println(getClass().getName());
    for (int i = 0; i < this.lockManagers.length; i++) {
      out.indent().println("[ " + i + "] = ");
      this.lockManagers[i].prettyPrint(out);
    }
    return out;
  }

}
