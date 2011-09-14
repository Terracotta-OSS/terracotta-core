/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.text.PrettyPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MockClientLockManager implements ClientLockManager {

  private final List begins     = new ArrayList();
  private int        unlockCount;
  private final List recallList = new ArrayList();

  public void clearBegins() {
    this.begins.clear();
  }

  public List getBegins() {
    List rv = new ArrayList();
    rv.addAll(this.begins);
    return rv;
  }

  public int getUnlockCount() {
    return unlockCount;
  }

  public void resetCounts() {
    unlockCount = 0;
    begins.clear();
  }

  public void award(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    throw new ImplementMe();
  }

  public void info(LockID lock, ThreadID requestor, Collection<ClientServerExchangeLockContext> contexts) {
    throw new ImplementMe();

  }

  public void notified(LockID lock, ThreadID thread) {
    throw new ImplementMe();
  }

  public void recall(NodeID node, SessionID session, LockID lock, ServerLockLevel level, int lease) {
    throw new ImplementMe();
  }

  public void recall(NodeID node, SessionID session, LockID lock, ServerLockLevel level, int lease, boolean batch) {
    recallList.add(lock);
  }

  public void refuse(NodeID node, SessionID session, LockID lock, ThreadID thread, ServerLockLevel level) {
    throw new ImplementMe();
  }

  public LockID generateLockIdentifier(String str) {
    throw new ImplementMe();
  }

  public LockID generateLockIdentifier(long l) {
    throw new ImplementMe();
  }

  public LockID generateLockIdentifier(Object obj) {
    throw new ImplementMe();
  }

  public LockID generateLockIdentifier(Object obj, String field) {
    throw new ImplementMe();
  }

  public int globalHoldCount(LockID lock, LockLevel level) {
    throw new ImplementMe();
  }

  public int globalPendingCount(LockID lock) {
    throw new ImplementMe();
  }

  public int globalWaitingCount(LockID lock) {
    throw new ImplementMe();
  }

  public boolean isLocked(LockID lock, LockLevel level) {
    throw new ImplementMe();
  }

  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    throw new ImplementMe();
  }

  public int localHoldCount(LockID lock, LockLevel level) {
    throw new ImplementMe();
  }

  public void lock(LockID lock, LockLevel level) {
    this.begins.add(new Begin(lock, level));
  }

  public void lockInterruptibly(LockID lock, LockLevel level) {
    throw new ImplementMe();
  }

  public Notify notify(LockID lock, Object waitObject) {
    throw new ImplementMe();
  }

  public Notify notifyAll(LockID lock, Object waitObject) {
    throw new ImplementMe();
  }

  public boolean tryLock(LockID lock, LockLevel level) {
    throw new ImplementMe();
  }

  public boolean tryLock(LockID lock, LockLevel level, long timeout) {
    throw new ImplementMe();
  }

  public void unlock(LockID lock, LockLevel level) {
    this.unlockCount++;
  }

  public void wait(LockID lock, Object waitObject) {
    throw new ImplementMe();
  }

  public void wait(LockID lock, Object waitObject, long timeout) {
    throw new ImplementMe();
  }

  public void wait(LockID lock, WaitListener listener) {
    throw new ImplementMe();
  }

  public void wait(LockID lock, WaitListener listener, long timeout) {
    throw new ImplementMe();
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    throw new ImplementMe();
  }

  public void pause(NodeID remoteNode, int disconnected) {
    throw new ImplementMe();
  }

  public void shutdown() {
    throw new ImplementMe();
  }

  public void unpause(NodeID remoteNode, int disconnected) {
    throw new ImplementMe();
  }

  public static class Begin {
    public LockID    lock;
    public LockLevel level;

    Begin(LockID lock, LockLevel level) {
      this.lock = lock;
      this.level = level;
    }
  }

  public Collection<ClientServerExchangeLockContext> getAllLockContexts() {
    throw new ImplementMe();
  }

  public int runLockGc() {
    throw new ImplementMe();
  }

  public void pinLock(LockID lock) {
    //
  }

  public void unpinLock(LockID lock) {
    //
  }

  public boolean isLockedByCurrentThread(LockLevel level) {
    return false;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    throw new ImplementMe();
  }

  public List<LockID> getRecallList() {
    return this.recallList;
  }

}
