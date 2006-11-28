/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
