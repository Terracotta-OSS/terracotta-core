/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.impl.GlobalLockInfo;
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

  public void unlock(LockID id, ThreadID threadID) {
    this.unlockCalls.add(new Object[] { id, threadID });
  }

  public LockID lockIDFor(String id) {
    this.lockIDForCalls.add(id);
    return new LockID(id);
  }

  public void awardLock(NodeID nid, SessionID sessionID, LockID id, ThreadID threadID, int type) {
    return;
  }

  public void lock(LockID id, ThreadID threadID, int lockType, String lockObjectType, String contextInfo) {
    this.locks.add(new Object[] { id, threadID, new Integer(lockType) });
  }

  public void wait(LockID lockID, ThreadID transactionID, TimerSpec call, Object waitLock, WaitListener listener) {
    this.waitCalls.add(new Object[] { lockID, transactionID, call, waitLock, listener });
  }

  public Notify notify(LockID lockID, ThreadID threadID, boolean all) {
    this.notifyCalls.add(new Object[] { lockID, threadID, new Boolean(all) });
    return Notify.NULL;
  }

  public void notified(LockID lockID, ThreadID threadID) {
    return;
  }

  public void recall(LockID lockID, ThreadID threadID, int level, int leaseTimeInMs) {
    return;
  }

  public void waitTimedOut(LockID lockID, ThreadID threadID) {
    return;
  }

  public boolean isLocked(LockID lockID, ThreadID threadID, int lockLevel) {
    return this.lockIDForCalls.contains(lockID.asString());
  }

  public int localHeldCount(LockID lockID, int lockLevel, ThreadID threadID) {
    throw new ImplementMe();
  }

  public void queryLockCommit(ThreadID threadID, GlobalLockInfo globalLockInfo) {
    throw new ImplementMe();
  }

  public int waitLength(LockID lockID, ThreadID threadID) {
    throw new ImplementMe();
  }

  public void lockInterruptibly(LockID id, ThreadID threadID, int lockType, String lockObjectType, String contextInfo) {
    throw new ImplementMe();
  }

  public boolean tryLock(LockID id, ThreadID threadID, TimerSpec timeout, int lockType, String lockObjectType) {
    throw new ImplementMe();
  }

  public int queueLength(LockID lockID, ThreadID threadID) {
    throw new ImplementMe();
  }

  public void cannotAwardLock(NodeID nid, SessionID sessionID, LockID id, ThreadID threadID, int type) {
    throw new ImplementMe();
  }

  public void requestLockSpecs() {
    throw new ImplementMe();
  }

  public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    throw new ImplementMe();
  }

  public void setLockStatisticsEnabled(boolean statEnable) {
    throw new ImplementMe();
  }

  public String dump() {
    throw new ImplementMe();
  }

  public void dumpToLogger() {
    throw new ImplementMe();
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new ImplementMe();
  }

  public void addAllLocksTo(LockInfoByThreadID lockIcnfo) {
    throw new ImplementMe();
  }
}
