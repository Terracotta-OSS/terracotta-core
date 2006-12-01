/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.exception.ImplementMe;
import com.tc.object.lockmanager.impl.GlobalLockInfo;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.WaitInvocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author steve
 */
public class TestLockManager implements ClientLockManager {
  public final List locks          = new ArrayList();
  public final List lockIDForCalls = new LinkedList();
  public final List waitCalls      = new LinkedList();
  public final List notifyCalls    = new LinkedList();
  public final List unlockCalls    = new LinkedList();

  public void unlock(LockID id, ThreadID threadID) {
    unlockCalls.add(new Object[] {id, threadID });
  }

  public LockID lockIDFor(String id) {
    lockIDForCalls.add(id);
    return new LockID(id);
  }

  public void awardLock(SessionID sessionID, LockID id, ThreadID threadID, int type) {
    return;
  }

  public void lock(LockID id, ThreadID threadID, int type) {
    locks.add(new Object[] { id, threadID, new Integer(type) });
  }

  public void wait(LockID lockID, ThreadID transactionID, WaitInvocation call, Object waitLock,
                   WaitListener listener) {
    waitCalls.add(new Object[] { lockID, transactionID, call, waitLock, listener });
  }

  public Notify notify(LockID lockID, ThreadID threadID, boolean all) {
    notifyCalls.add(new Object[] { lockID, threadID, new Boolean(all) });
    return Notify.NULL;
  }

  public Collection addAllPendingLockRequestsTo(Collection c) {
    return c;
  }

  public void pause() {
    return;
  }

  public void starting() {
    return;
  }
  public void unpause() {
    return;

  }

  public boolean isStarting() {
    return false;
  }

  public Collection addAllWaitersTo(Collection c) {
    return c;
  }

  public Collection addAllHeldLocksTo(Collection c) {
    return c;
  }

  public void notified(LockID lockID, ThreadID threadID) {
    return;
  }

  public void recall(LockID lockID, ThreadID id, int level) {
    return;
  }

  public void waitTimedout(LockID lockID, ThreadID threadID) {
    return;
  }

  public void runGC() {
    return;
  }

  public boolean isLocked(LockID lockID) {
    return lockIDForCalls.contains(lockID.asString());
  }

  public int queueLength(LockID lockID, ThreadID threadID) {
    throw new ImplementMe();
  }

  public int heldCount(LockID lockID, int lockLevel, ThreadID threadID) {
    throw new ImplementMe();
  }

  public boolean isLocked(LockID lockID, ThreadID threadID) {
    throw new ImplementMe();
  }

  public boolean haveLock(LockID lockID, TransactionID requesterID) {
    throw new ImplementMe();
  }

  public void queryLockCommit(ThreadID threadID, GlobalLockInfo globalLockInfo) {
    throw new ImplementMe();
  }

  public int waitLength(LockID lockID, ThreadID threadID) {
    throw new ImplementMe();
  }

  public boolean tryLock(LockID id, ThreadID threadID, int type) {
    throw new ImplementMe();
  }

  public void cannotAwardLock(SessionID sessionID, LockID id, ThreadID threadID, int type) {
    throw new ImplementMe();
    
  }
}
