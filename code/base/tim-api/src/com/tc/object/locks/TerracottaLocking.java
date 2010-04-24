/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

public interface TerracottaLocking {
  /**
   * Blocking acquire of a Terracotta lock.
   * 
   * @param lock lock to act upon
   * @param level level at which to lock
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   */
  public void lock(LockID lock, LockLevel level);

  /**
   * Try to acquire a Terracotta lock.
   * <p>
   * Non-blocking try acquires will wait for a definitive server response - in this sense they are not truly
   * non-blocking...
   * 
   * @param lock lock to act upon
   * @param level level at which to lock
   * @return <code>true</code> if locked
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   */
  public boolean tryLock(LockID lock, LockLevel level);

  /**
   * Timed acquire of a Terracotta lock.
   * 
   * @param lock lock to act upon
   * @param level level at which to lock
   * @param timeout maximum time to wait in milliseconds
   * @return <code>true</code> if locked
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   */
  public boolean tryLock(LockID lock, LockLevel level, long timeout) throws InterruptedException;

  /**
   * Interruptible acquire of a Terracotta lock.
   * 
   * @param lock lock to act upon
   * @param level level at which to lock
   * @throws TCLockUpgradeNotSupportedError on attempting to read&rarr;write upgrade
   */
  public void lockInterruptibly(LockID lock, LockLevel level) throws InterruptedException;

  /**
   * Blocking unlock of a Terracotta lock.
   * 
   * @param lock lock to act upon
   * @param level at which to unlock
   * @throws IllegalMonitorStateException if there is no matching lock hold
   */
  public void unlock(LockID lock, LockLevel level);

  /**
   * Notify a single thread waiting on the given lock.
   * 
   * @param lock lock to act upon
   * @param waitObject local vm object on which threads are waiting
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public Notify notify(LockID lock, Object waitObject);

  /**
   * Notify all threads waiting on the given lock.
   * 
   * @param lock lock to act upon
   * @param waitObject local vm object on which threads are waiting
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public Notify notifyAll(LockID lock, Object waitObject);

  /**
   * Move the current thread to wait on the given lock.
   * 
   * @param lock lock to act upon
   * @param waitObject local vm object to wait on
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public void wait(LockID lock, Object waitObject) throws InterruptedException;

  /**
   * Move the current thread to wait on the given lock with timeout.
   * 
   * @param lock lock to act upon
   * @param waitObject local vm object to wait on
   * @param timeout maximum time to remain waiting
   * @throws IllegalMonitorStateException if the current thread does not hold a write lock
   */
  public void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException;

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
   */
  public boolean isLocked(LockID lock, LockLevel level);

  /**
   * Return true if the given lock is held by the current thread at the given lock level.
   * <p>
   * It is also important to note that the current locking implementation <em>does not</em> track concurrent lock holds.
   * 
   * @param lock lock to query
   * @param level level to query
   */
  public boolean isLockedByCurrentThread(LockID lock, LockLevel level);

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
   */
  public int localHoldCount(LockID lock, LockLevel level);

  /**
   * Return the count of global (cluster-wide) holders at the given lock level.
   * <p>
   * This method has unusual semantics similar to isLocked.
   * 
   * @see TerracottaLocking#isLocked(LockID, LockLevel)
   * @param lock lock to query
   * @param level level to query
   */
  public int globalHoldCount(LockID lock, LockLevel level);

  /**
   * Return the count of global (cluster-wide) pending holders.
   * <p>
   * This method has unusual semantics similar to isLocked.
   * 
   * @see TerracottaLocking#isLocked(LockID, LockLevel)
   * @param lock lock to query
   */
  public int globalPendingCount(LockID lock);

  /**
   * Return the count of global (cluster-wide) waiting threads.
   * <p>
   * This method has unusual semantics similar to isLocked.
   * 
   * @see TerracottaLocking#isLocked(LockID, LockLevel)
   * @param lock lock to query
   */
  public int globalWaitingCount(LockID lock);

  public void pinLock(LockID lock);

  public void unpinLock(LockID lock);

  public LockID generateLockIdentifier(String str);

  public LockID generateLockIdentifier(Object obj);

  public LockID generateLockIdentifier(Object obj, String field);
}
