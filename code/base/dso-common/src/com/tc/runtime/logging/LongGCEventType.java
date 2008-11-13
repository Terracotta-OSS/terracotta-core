/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
