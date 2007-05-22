/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent.locks;

import java.util.concurrent.locks.Lock;

public interface TCLock extends Lock {
  public Object getTCLockingObject();
  
  public boolean isHeldByCurrentThread();
  
  public int localHeldCount();
}
