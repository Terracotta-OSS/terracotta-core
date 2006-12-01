/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;


/**
 * Lock's management interface
 */
public interface LockMBean {

  String getLockName();

  LockHolder[] getHolders();

  ServerLockRequest[] getPendingRequests();

  ServerLockRequest[] getPendingUpgrades();

  Waiter[] getWaiters();

}
