/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.impl.GlobalLockInfo;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TimerSpec;
import com.tc.text.PrettyPrinter;
import com.tc.util.runtime.LockInfoByThreadID;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestLockManager implements ClientLockManager {
  public final List locks          = new ArrayList();
  public final List lockIDForCalls = new LinkedList();
  public final List waitCalls      = new LinkedList();
  public final List notifyCalls    = new LinkedList();
  public final List unlockCalls    = new LinkedList();

  public void unlock(final LockID id, final ThreadID threadID) {
    this.unlockCalls.add(new Object[] { id, threadID });
  }

  public LockID lockIDFor(final String id) {
    this.lockIDForCalls.add(id);
    return new LockID(id);
  }

  public void awardLock(final NodeID nid, final SessionID sessionID, final LockID id, final ThreadID threadID,
                        final int type) {
    return;
  }

  public void lock(final LockID id, final ThreadID threadID, final int lockType, final String lockObjectType,
                   final String contextInfo) {
    this.locks.add(new Object[] { id, threadID, new Integer(lockType) });
  }

  public void wait(final LockID lockID, final ThreadID transactionID, final TimerSpec call, final Object waitLock,
                   final WaitListener listener) {
    this.waitCalls.add(new Object[] { lockID, transactionID, call, waitLock, listener });
  }

  public Notify notify(final LockID lockID, final ThreadID threadID, final boolean all) {
    this.notifyCalls.add(new Object[] { lockID, threadID, new Boolean(all) });
    return Notify.NULL;
  }

  public void notified(final LockID lockID, final ThreadID threadID) {
    return;
  }

  public void recall(final LockID lockID, final ThreadID threadID, final int level, final int leaseTimeInMs) {
    return;
  }

  public void waitTimedOut(final LockID lockID, final ThreadID threadID) {
    return;
  }

  public boolean isLocked(final LockID lockID, final ThreadID threadID, final int lockLevel) {
    return this.lockIDForCalls.contains(lockID.asString());
  }

  public int localHeldCount(final LockID lockID, final int lockLevel, final ThreadID threadID) {
    throw new ImplementMe();
  }

  public void queryLockCommit(final ThreadID threadID, final GlobalLockInfo globalLockInfo) {
    throw new ImplementMe();
  }

  public int waitLength(final LockID lockID, final ThreadID threadID) {
    throw new ImplementMe();
  }

  public void lockInterruptibly(final LockID id, final ThreadID threadID, final int lockType,
                                final String lockObjectType, final String contextInfo) {
    throw new ImplementMe();
  }

  public boolean tryLock(final LockID id, final ThreadID threadID, final TimerSpec timeout, final int lockType,
                         final String lockObjectType) {
    throw new ImplementMe();
  }

  public int queueLength(final LockID lockID, final ThreadID threadID) {
    throw new ImplementMe();
  }

  public void cannotAwardLock(final NodeID nid, final SessionID sessionID, final LockID id, final ThreadID threadID,
                              final int type) {
    throw new ImplementMe();
  }

  public void requestLockSpecs() {
    throw new ImplementMe();
  }

  public void setLockStatisticsConfig(final int traceDepth, final int gatherInterval) {
    throw new ImplementMe();
  }

  public void setLockStatisticsEnabled(final boolean statEnable) {
    throw new ImplementMe();
  }

  public String dump() {
    throw new ImplementMe();
  }

  public void dumpToLogger() {
    throw new ImplementMe();
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    throw new ImplementMe();
  }

  public void addAllLocksTo(final LockInfoByThreadID lockIcnfo) {
    throw new ImplementMe();
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    throw new ImplementMe();
  }

  public void pause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();
  }

  public void unpause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();
  }
}
