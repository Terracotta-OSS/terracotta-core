/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.net.ClientID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.locks.timer.TimerCallback;

import java.util.Collection;

public interface ServerLock extends TimerCallback {
  enum NotifyAction {
    ONE, ALL
  }

  /**
   * This method is called when a clustered thread wants to acquire this lock
   * 
   * @param cid - Id of the client requesting
   * @param tid - Id of the thread requesting
   * @param level - read or write
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  void lock(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper);

  /**
   * This method is called when a clustered thread tries to acquire this lock
   * 
   * @param cid - Id of the client requesting
   * @param tid - Id of the thread requesting
   * @param level - read or write
   * @param timeout - in millis. The time for which the server should wait before refusing.
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  void tryLock(ClientID cid, ThreadID tid, ServerLockLevel level, long timeout, LockHelper helper);

  /**
   * Informs the client the current state of the lock
   * 
   * @param cid - Id of the client requesting
   * @param tid - Id of the thread requesting
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  void queryLock(ClientID cid, ThreadID tid, LockHelper helper);

  /**
   * Interrupts the client thread waiting on this lock.
   * 
   * @param cid - Id of the client requesting
   * @param tid - Id of the thread requesting
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  void interrupt(ClientID cid, ThreadID tid, LockHelper helper);

  /**
   * Releases the lock held by the client thread.
   * 
   * @param cid - Id of the client requesting
   * @param tid - Id of the thread requesting
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  void unlock(ClientID cid, ThreadID tid, LockHelper helper);

  /**
   * This method is called when a reply comes from the client after being asked to give up the greedy lock. The client
   * sends its state in form of contexts to the server.
   * 
   * @param cid - Id of the client requesting
   * @param serverLockContexts - Contexts that were present on the client side
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  void recallCommit(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts, LockHelper helper);

  /**
   * This method is called to notify threads waiting on this lock.
   * 
   * @param cid - Id of the client requesting
   * @param tid - Id of the thread requesting
   * @param action - all or one (notifyAll or just notify)
   * @param addNotifiedWaitersTo - list to add the waiters which were notified due to this call
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  NotifiedWaiters notify(ClientID cid, ThreadID tid, NotifyAction action, NotifiedWaiters addNotifiedWaitersTo,
                         LockHelper helper);

  /**
   * This method is called when a clustered thread wants to wait on this lock. The support for wait(timeout) is also
   * supported.
   * 
   * @param cid - Id of the client requesting
   * @param tid - Id of the thread requesting
   * @param timeout - in millis. The time for which the server should wait before refusing.
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  void wait(ClientID cid, ThreadID tid, long timeout, LockHelper helper);

  /**
   * This method is called during handshake when the client informs the server of its locks. This method will only be
   * called with holders context and waiters.
   * 
   * @param serverLockContext - Id of the client requesting
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  void reestablishState(ClientServerExchangeLockContext serverLockContext, LockHelper lockHelper);

  /**
   * On client disconnect this method will be called. This will clear all the state being stored for this particular
   * client.
   * 
   * @param cid - Id of the client requesting
   * @param helper - Helps getting stats manager, state machine, etc.
   */
  boolean clearStateForNode(ClientID cid, LockHelper helper);

  /**
   * Returns the state of the lock in bean form.
   * 
   * @param channelManager - channel manager for obtaining the address from client id.
   */
  LockMBean getMBean(DSOChannelManager channelManager);

  /**
   * Returns the lock id associated with this lock
   */
  LockID getLockID();
}
