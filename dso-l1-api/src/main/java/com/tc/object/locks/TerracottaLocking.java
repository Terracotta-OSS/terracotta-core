/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.locks;

import com.tc.abortable.AbortedOperationException;
import com.tc.exception.TCLockUpgradeNotSupportedError;

public interface TerracottaLocking {
  /**
   * Blocking acquire of a Terracotta lock.
   * 
   * @param lock lock to act upon
   * @param level level at which to lock
   * @throws AbortedOperationException
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   */
  public void lock(LockID lock, LockLevel level) throws AbortedOperationException;

  /**
   * Try to acquire a Terracotta lock.
   * <p>
   * Non-blocking try acquires will wait for a definitive server response - in this sense they are not truly
   * non-blocking...
   * 
   * @param lock lock to act upon
   * @param level level at which to lock
   * @return <code>true</code> if locked
   * @throws AbortedOperationException
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   */
  public boolean tryLock(LockID lock, LockLevel level) throws AbortedOperationException;

  /**
   * Timed acquire of a Terracotta lock.
   * 
   * @param lock lock to act upon
   * @param level level at which to lock
   * @param timeout maximum time to wait in milliseconds
   * @return <code>true</code> if locked
   * @throws AbortedOperationException
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   */
  public boolean tryLock(LockID lock, LockLevel level, long timeout) throws InterruptedException,
      AbortedOperationException;

  /**
   * Interruptible acquire of a Terracotta lock.
   * 
   * @param lock lock to act upon
   * @param level level at which to lock
   * @throws AbortedOperationException
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   */
  public void lockInterruptibly(LockID lock, LockLevel level) throws InterruptedException, AbortedOperationException;

  /**
   * Blocking unlock of a Terracotta lock.
   * 
   * @param lock lock to act upon
   * @param level at which to unlock
   * @throws AbortedOperationException
   * @throws IllegalMonitorStateException if there is no matching lock hold
   */
  public void unlock(LockID lock, LockLevel level) throws AbortedOperationException;

  /**
   * Notify a single thread waiting on the given lock.
   * 
   * @param lock lock to act upon
   * @param waitObject local vm object on which threads are waiting
   * @throws AbortedOperationException
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public Notify notify(LockID lock, Object waitObject) throws AbortedOperationException;

  /**
   * Notify all threads waiting on the given lock.
   * 
   * @param lock lock to act upon
   * @param waitObject local vm object on which threads are waiting
   * @throws AbortedOperationException
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public Notify notifyAll(LockID lock, Object waitObject) throws AbortedOperationException;

  /**
   * Move the current thread to wait on the given lock.
   * 
   * @param lock lock to act upon
   * @param waitObject local vm object to wait on
   * @throws AbortedOperationException
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public void wait(LockID lock, Object waitObject) throws InterruptedException, AbortedOperationException;

  /**
   * Move the current thread to wait on the given lock with timeout.
   * 
   * @param lock lock to act upon
   * @param waitObject local vm object to wait on
   * @param timeout maximum time to remain waiting
   * @throws AbortedOperationException
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException, AbortedOperationException;

  /**
   * Return true if the given lock is held by any thread at the given lock level.
   * <p>
   * The semantics of this method are fairly loose when using a greedy locking policy. It is assumed that a client
   * holding a greedy lock has in turn one holding thread of each possible type.
   * <p>
   * It is also important to note that the current locking implementation <em>does not</em> track concurrent lock holds.
   * 
   * @param lock lock to query
   * @param level level to query
   * @throws AbortedOperationException
   */
  public boolean isLocked(LockID lock, LockLevel level) throws AbortedOperationException;

  /**
   * Return true if the given lock is held by the current thread at the given lock level.
   * <p>
   * It is also important to note that the current locking implementation <em>does not</em> track concurrent lock holds.
   * 
   * @param lock lock to query
   * @param level level to query
   * @throws AbortedOperationException
   */
  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) throws AbortedOperationException;

  /**
   * Return true if any lock is held by the current thread at the given lock level.
   * <p>
   * It is also important to note that the current locking implementation <em>does not</em> track concurrent lock holds.
   * 
   * @param level level to query
   */
  public boolean isLockedByCurrentThread(LockLevel level);

  /**
   * Return the count of local (on this client VM) holders at the given lock level.
   * 
   * @param lock lock to query
   * @param level level to query
   * @throws AbortedOperationException
   */
  public int localHoldCount(LockID lock, LockLevel level) throws AbortedOperationException;

  /**
   * Return the count of global (cluster-wide) holders at the given lock level.
   * <p>
   * This method has unusual semantics similar to isLocked.
   * 
   * @see TerracottaLocking#isLocked(LockID, LockLevel)
   * @param lock lock to query
   * @param level level to query
   * @throws AbortedOperationException
   */
  public int globalHoldCount(LockID lock, LockLevel level) throws AbortedOperationException;

  /**
   * Return the count of global (cluster-wide) pending holders.
   * <p>
   * This method has unusual semantics similar to isLocked.
   * 
   * @see TerracottaLocking#isLocked(LockID, LockLevel)
   * @param lock lock to query
   * @throws AbortedOperationException
   */
  public int globalPendingCount(LockID lock) throws AbortedOperationException;

  /**
   * Return the count of global (cluster-wide) waiting threads.
   * <p>
   * This method has unusual semantics similar to isLocked.
   * 
   * @see TerracottaLocking#isLocked(LockID, LockLevel)
   * @param lock lock to query
   * @throws AbortedOperationException
   */
  public int globalWaitingCount(LockID lock) throws AbortedOperationException;

  public void pinLock(LockID lock, long awardID);

  public void unpinLock(LockID lock, long awardID);

  public LockID generateLockIdentifier(String str);

  public LockID generateLockIdentifier(Object obj);

  public LockID generateLockIdentifier(Object obj, String field);

  public LockID generateLockIdentifier(long l);
}
