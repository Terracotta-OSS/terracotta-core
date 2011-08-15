/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

public class QueueEvent {
  public static final int SERVER_CRASH   = 0;
  public static final int SERVER_RESTART = 1;
  private final int       action;

  public QueueEvent(int action) {
    if (action == SERVER_CRASH || action == SERVER_RESTART) {
      this.action = action;
    } else {
      throw new AssertionError("Cannot create QueueEvent: Unrecognized action type: " + action);
    }
  }

  public int getAction() {
    return this.action;
  }

}
