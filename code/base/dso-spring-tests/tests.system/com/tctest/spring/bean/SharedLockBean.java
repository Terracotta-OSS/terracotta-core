/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.bean;

import java.util.UUID;

public class SharedLockBean implements ISharedLock {
  // shared variables
  private int                        val             = 0;
  private UUID                       firstHolder     = null;

  // variables not shared
  private volatile transient boolean release         = false;
  private volatile transient boolean holdsSharedLock = false;
  private final transient Object     localLock       = new Object();
  private final transient UUID       localID         = UUID.randomUUID();

  public UUID getLocalID() {
    return localID;
  }

  public void lockAndMutate() {
    try {
      synchronized (firstHolder) {
        holdsSharedLock = true;

        if (firstHolder == null) {
          firstHolder = new UUID(localID.getMostSignificantBits(), localID.getLeastSignificantBits());

          while (!release) {
            sleep(100);
          }

        } else if (firstHolder.equals(localID)) {
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

  public UUID getFirstHolder() {
    synchronized (firstHolder) {
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
      val++;
    } catch (RuntimeException e) {
      if (e.getClass().getName().equals("com.tc.object.tx.UnlockedSharedObjectException")) {
        // expected
      } else {
        throw e;
      }
    }

    if (val != 0) { throw new AssertionError("variable changed to " + val); }
  }

  public boolean isFirstHolder() {
    synchronized (localLock) {
      if (firstHolder == null) { return false; }
      return firstHolder.equals(localID);
    }
  }
}
