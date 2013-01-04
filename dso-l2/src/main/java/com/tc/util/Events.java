/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util;

/**
 * Static utility methods to access event instances.
 *
 * @author Eugene Shelestovich
 */
public final class Events {

  private Events() {}

  /**
   * Constructs an event what notifies subscribers about new map mutation operation.
   */
  public static OperationCountIncrementEvent operationCountIncrementEvent() {
    return OperationCountIncrementEvent.INSTANCE;
  }

  /**
   * Constructs an event what notifies subscribers about new map mutation operations.
   */
  public static OperationCountChangeEvent operationCountChangeEvent(int delta) {
    return new OperationCountChangeEvent(delta);
  }

  /**
   * Immutable event, can be cached.
   */
  public static enum OperationCountIncrementEvent {
    INSTANCE
  }

  public static final class OperationCountChangeEvent {
    private final int delta;

    OperationCountChangeEvent(final int delta) {
      this.delta = delta;
    }

    public int getDelta() {
      return delta;
    }
  }
}
