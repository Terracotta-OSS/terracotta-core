/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

/**
 * An event which notifies subscribers about map mutation operations.
 *
 * @author Eugene Shelestovich
 */
public final class OperationCountChangeEvent {

  private final int delta;

  public OperationCountChangeEvent() {
    this.delta = 1;
  }

  public OperationCountChangeEvent(final int delta) {
    this.delta = delta;
  }

  public int getDelta() {
    return delta;
  }
}
