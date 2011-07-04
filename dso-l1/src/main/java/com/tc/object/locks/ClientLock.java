/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.net.ClientID;
import com.tc.object.msg.ClientHandshakeMessage;

import java.util.Collection;

public interface ClientLock {
  /**
   * Blocking acquire
   * 
   * @param remote remote lock manager for delegation
   * @param thread id of the locking (current) thread
   * @param level level at which to lock
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   * @throws GarbageLockException if this state has been marked as garbage
   */
  public void lock(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException;

  /**
   * Try to acquire
   * <p>
   * Non-blocking try acquires will wait for a definitive server response - in
   * this sense they are not truly non-blocking...
   * 
   * @param remote remote lock manager for delegation
   * @param thread id of the locking (current) thread
   * @param level level at which to lock
   * @return <code>true</code> if locked
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   * @throws GarbageLockException if this state has been marked as garbage
   */
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException;
  
  /**
   * Timed acquire
   * 
   * @param remote remote lock manager for delegation
   * @param thread id of the locking (current) thread
   * @param level level at which to lock
   * @param timeout maximum time to wait in milliseconds
   * @return <code>true</code> if locked
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   * @throws GarbageLockException if this state has been marked as garbage
   */
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws InterruptedException, GarbageLockException;
  
  /**
   * Interruptible acquire
   * 
   * @param remote remote lock manager for delegation
   * @param thread id of the locking (current) thread
   * @param level level at which to lock
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   * @throws GarbageLockException if this state has been marked as garbage
   */
  public void lockInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException, GarbageLockException;

  /**
   * Blocking unlock
   * 
   * @param remote remote lock manager for delegation
   * @param thread id of the unlocking (current) thread
   * @param level at which to unlock
   * @throws IllegalMonitorStateException if there is no matching lock hold
   */
  public void unlock(RemoteLockManager remote, ThreadID thread, LockLevel level);

  /**
   * Notify a single thread waiting on the lock.
   *
   * @param remote remote lock manager for delegation
   * @param thread id of the locking (current) thread
   * @param waitObject TODO
   * @return <code>true</code> is remote threads may need notifying
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public boolean notify(RemoteLockManager remote, ThreadID thread, Object waitObject);
  
  /**
   * Notify all threads waiting on the lock.
   * 
   * @param remote remote lock manager for delegation
   * @param thread id of the locking (current) thread
   * @param waitObject TODO
   * @return <code>true</code> is remote threads may need notifying
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */  
  public boolean notifyAll(RemoteLockManager remote, ThreadID thread, Object waitObject);
  
  /**
   * Move the current thread to wait
   * 
   * @param remote remote lock manager for delegation
   * @param listener listener to fire just prior to moving to a local JVM Object.wait();
   * @param thread id of the locking (current) thread
   * @param waitObject TODO
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, Object waitObject) throws InterruptedException;
  /**
   * Move the current thread to wait with timeout.
   * 
   * @param remote remote lock manager for delegation
   * @param listener listener to fire just prior to moving to a local JVM Object.wait();
   * @param thread id of the locking (current) thread
   * @param waitObject TODO
   * @param timeout maximum time to remain waiting
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */  
  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, Object waitObject, long timeout) throws InterruptedException;

  /**
   * Return true if the given lock is held locally by any thread at the given lock level.
   * <p>
   * It is also important to note that the current locking implementation <em>does not</em>
   * track concurrent lock holds. 
   * 
   * @param level level to query
   */
  public boolean isLocked(LockLevel level);
    
  /**
   * Return true if the given lock is held locally by the given thread at the given lock level.
   * <p>
   * It is also important to note that the current locking implementation <em>does not</em>
   * track concurrent lock holds. 
   * 
   * @param thread thread id to query
   * @param level level to query
   */
  public boolean isLockedBy(ThreadID thread, LockLevel level);

  /**
   * Return the count of local (on this client VM) holders at the given lock level.
   * 
   * @param level level to query
   */
  public int holdCount(LockLevel level);
  
  /**
   * Return the count of local pending holders.
   */
  public int pendingCount();
  
  /**
   * Return the count of local waiters.
   */
  public int waitingCount();

  /**
   * Called by a Terracotta thread to notify the given thread waiting on the lock.
   */
  public void notified(ThreadID thread);

  /**
   * Called by a Terracotta thread to request the return of a greedy lock previously
   * awarded to the client.
   */
  public boolean recall(RemoteLockManager remote, ServerLockLevel interest, int lease, boolean batch);

  /**
   * Called by a Terracotta thread to award a per-thread or greedy lock to the client.
   * 
   * @throws GarbageLockException if this state has been marked as garbage
   */
  public void award(RemoteLockManager remote, ThreadID thread, ServerLockLevel level) throws GarbageLockException;

  /**
   * Called by a Terracotta thread to indicate that the specified non-blocking try lock attempt
   * has failed at the server.
   */
  public void refuse(ThreadID thread, ServerLockLevel level);

  /**
   * Dump the entire lock state as a collection of {@link ClientServerExchangeLockContext} objects.
   * <p>
   * The dumped state should include as much of the following as the implementation allows:
   * <ul>
   * <li>all thread holds (including nested holds)</li>
   * <li>greedy holds</li>
   * <li>pending (queued) locks</li>
   * <li>waiting threads</li>
   * </ul>
   */
  public Collection<ClientServerExchangeLockContext> getStateSnapshot(ClientID client);
  
  /**
   * Add the necessary current lock state information to the handshake message. 
   * 
   * @param handshake message to add state to
   */  
  public void initializeHandshake(ClientID client, ClientHandshakeMessage handshake);

  /**
   * ClientLock implementations must return true (and subsequently throw GarbageLockException) if
   * they consider themselves garbage.
   * @param remote remote manager to interact with
   */
  public boolean tryMarkAsGarbage(RemoteLockManager remote);

  public void pinLock();

  public void unpinLock();
}
