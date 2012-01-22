/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime.logging;

public final class LongGCEventType {
  public static final LongGCEventType LONG_GC = new LongGCEventType("LONG_GC");

  private final String                       name;

  private LongGCEventType(String name) {
    this.name = name;
  }

  public String toString() {
    return "LongGCEventType." + name;
  }
}
