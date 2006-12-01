/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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