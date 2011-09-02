/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.text.PrettyPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TestLockManager implements ClientLockManager {
  public final List locks          = new ArrayList();
  public final List lockIDForCalls = new LinkedList();
  public final List waitCalls      = new LinkedList();
  public final List notifyCalls    = new LinkedList();
  public final List unlockCalls    = new LinkedList();

  public void unlock(final LockID id, final LockLevel level) {
    this.unlockCalls.add(new Object[] { id, level });
  }

  public LockID lockIDFor(final String id) {
    this.lockIDForCalls.add(id);
    return new StringLockID(id);
  }

  public void award(final NodeID nid, final SessionID sessionID, final LockID id, final ThreadID threadID,
                    final ServerLockLevel level) {
    return;
  }

  public void lock(final LockID id, final LockLevel level) {
    this.locks.add(new Object[] { id, level });
  }

  public void wait(final LockID lockID, Object waitObject) {
    this.waitCalls.add(new Object[] { lockID, null });
  }

  public void wait(final LockID lockID, Object waitObject, final long timeout) {
    this.waitCalls.add(new Object[] { lockID, Long.valueOf(timeout) });
  }

  public void wait(final LockID lockID, WaitListener listener) {
    this.waitCalls.add(new Object[] { lockID, null });
  }

  public void wait(final LockID lockID, WaitListener listener, final long timeout) {
    this.waitCalls.add(new Object[] { lockID, Long.valueOf(timeout) });
  }

  public Notify notify(final LockID lockID, Object waitObject) {
    this.notifyCalls.add(new Object[] { lockID, Boolean.FALSE });
    return null;
  }

  public Notify notifyAll(final LockID lockID, Object waitObject) {
    this.notifyCalls.add(new Object[] { lockID, Boolean.TRUE });
    return null;
  }

  public void notified(final LockID lockID, final ThreadID threadID) {
    return;
  }

  public void recall(final NodeID node, final SessionID session, final LockID lockID, final ServerLockLevel level,
                     final int leaseTime) {
    return;
  }

  public void recall(final NodeID node, final SessionID session, final LockID lockID, final ServerLockLevel level,
                     final int leaseTime, boolean batch) {
    return;
  }

  public void waitTimedOut(final LockID lockID, final ThreadID threadID) {
    return;
  }

  public boolean isLocked(final LockID lockID, final LockLevel lockLevel) {
    return this.lockIDForCalls.contains(lockID);
  }

  public int localHoldCount(final LockID lockID, final LockLevel lockLevel) {
    throw new ImplementMe();
  }

  public int globalHoldCount(final LockID lockID, final LockLevel lockLevel) {
    throw new ImplementMe();
  }

  public void info(final LockID lock, final ThreadID threadID,
                   final Collection<ClientServerExchangeLockContext> contexts) {
    throw new ImplementMe();
  }

  public int globalWaitingCount(final LockID lockID) {
    throw new ImplementMe();
  }

  public void lockInterruptibly(final LockID id, final LockLevel lockType) {
    throw new ImplementMe();
  }

  public boolean tryLock(final LockID id, final LockLevel lockType) {
    throw new ImplementMe();
  }

  public boolean tryLock(final LockID id, final LockLevel lockType, long timeout) {
    throw new ImplementMe();
  }

  public int globalPendingCount(final LockID lockID) {
    throw new ImplementMe();
  }

  public void refuse(final NodeID nid, final SessionID sessionID, final LockID id, final ThreadID threadID,
                     final ServerLockLevel type) {
    throw new ImplementMe();
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new ImplementMe();
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    throw new ImplementMe();
  }

  public void shutdown() {
    // NOP
  }

  public void pause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();
  }

  public void unpause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();
  }

  public LockID generateLockIdentifier(long l) {
    throw new ImplementMe();
  }

  public LockID generateLockIdentifier(String str) {
    throw new ImplementMe();
  }

  public LockID generateLockIdentifier(Object obj) {
    throw new ImplementMe();
  }

  public LockID generateLockIdentifier(Object obj, String field) {
    throw new ImplementMe();
  }

  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    throw new ImplementMe();
  }

  public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
    throw new ImplementMe();
  }

  public int runLockGc() {
    throw new ImplementMe();
  }

  public void pinLock(LockID lock) {
    throw new ImplementMe();

  }

  public void unpinLock(LockID lock) {
    throw new ImplementMe();

  }

  public boolean isLockedByCurrentThread(LockLevel level) {
    throw new ImplementMe();
  }

}
