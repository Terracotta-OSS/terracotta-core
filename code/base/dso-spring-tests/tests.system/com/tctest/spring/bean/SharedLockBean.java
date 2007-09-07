/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.bean;

import com.tc.aspectwerkz.proxy.Uuid;

public class SharedLockBean implements ISharedLock {
  // shared variables
  private int                        i               = 0;
  private Long                       firstHolder     = null;

  // variables not shared
  private volatile transient boolean release         = false;
  private volatile transient boolean holdsSharedLock = false;
  private final transient Object     localLock       = new Object();
  private final transient long       localID         = System.identityHashCode(this) + Uuid.newUuid();

  public long getLocalID() {
    return localID;
  }

  public void lockAndMutate() {
    try {
      synchronized (this) {
        holdsSharedLock = true;

        if (firstHolder == null) {
          synchronized (localLock) {
            firstHolder = new Long(localID);
          }

          while (!release) {
            sleep(100);
          }

        } else if (firstHolder.equals(new Long(localID))) {
          //
          throw new AssertionError("firstholder was me!");
        }

      }
    } finally {
      holdsSharedLock = false;
    }
  }

  public boolean sharedLockHeld() {
    return holdsSharedLock;
  }

  public void release() {
    release = true;
  }

  public Long getFirstHolder() {
    synchronized (localLock) {
      return firstHolder;
    }
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void unlockedMutate() {
    try {
      i++;
    } catch (RuntimeException e) {
      if (e.getClass().getName().equals("com.tc.object.tx.UnlockedSharedObjectException")) {
        // expected
      } else {
        throw e;
      }
    }

    if (i != 0) { throw new AssertionError("variable changed to " + i); }
  }

  public boolean isFirstHolder() {
    synchronized (localLock) {
      if (firstHolder == null) { return false; }
      return firstHolder.longValue() == localID;
    }
  }
}
