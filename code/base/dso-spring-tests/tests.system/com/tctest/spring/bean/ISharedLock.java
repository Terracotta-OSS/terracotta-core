/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.bean;

import java.util.UUID;

public interface ISharedLock {

  public UUID getLocalID();

  public void unlockedMutate();

  public void lockAndMutate();

  public void release();

  public UUID getFirstHolder();

  public boolean isFirstHolder();

  public boolean sharedLockHeld();

}