/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

public class LockState {
  public static final LockState HOLDING    = new LockState("HOLDING LOCK");
  public static final LockState WAITING_ON = new LockState("WAITING ON LOCK");
  public static final LockState WAITING_TO = new LockState("WAITING TO LOCK");

  private final String          state;

  private LockState(String state) {
    this.state = state;
  }

  public String toString() {
    return this.state;
  }

}
