/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

public class LockState {
  public static final LockState HOLDING    = new LockState();
  public static final LockState WAITING_ON = new LockState();
  public static final LockState WAITING_TO = new LockState();

  private LockState() {
    // do nothing -- to prevent from any instantiation of this class
  }

}
