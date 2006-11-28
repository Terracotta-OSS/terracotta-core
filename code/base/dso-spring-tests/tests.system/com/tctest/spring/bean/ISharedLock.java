/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

import java.util.List;

public interface ISharedLock {

  public void start();

  public void moveToStep(int step);

  public List getSharedVar();

  public List gethUnSharedVar();
  
  public long getLocalID();

}