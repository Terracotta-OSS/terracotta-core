/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

/**
 * Listener interface for lock events.
 */
public interface LockEventListener {

  // NOTE: There are more lock events (upgrades, wait/notify, etc.) that could be added if need be
  
  /**
   * Notification that there is a new pending request for the lock indicated in the given lock award context.
   * 
   * @param waiterCount The number of pending requests currently waiting for the lock-- including the one just added.
   * @param ctxt The award context of the current holder -- NOT the waiter.
   */
  public void notifyAddPending(int waiterCount, LockAwardContext ctxt);

  /**
   * Notification that a lock was awarded in the given award context.
   * 
   * @param waiterCount The number of waiters currently waiting for the lock after the award.
   * @param ctxt The award context of this lock award event.
   */
  public void notifyAward(int waiterCount, LockAwardContext ctxt);

  /**
   * Notification that a lock has been revoked in the given lock award context.
   * 
   * @param ctxt The award context that is being revoked. This should be the holder prior to revocation.
   */
  public void notifyRevoke(LockAwardContext ctxt);
}