/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.bean;

public interface ISharedLock {

  public long getLocalID();

  public void unlockedMutate();

  public void lockAndMutate();

  public void release();

  public Long getFirstHolder();

  public boolean isFirstHolder();

  public boolean sharedLockHeld();

}