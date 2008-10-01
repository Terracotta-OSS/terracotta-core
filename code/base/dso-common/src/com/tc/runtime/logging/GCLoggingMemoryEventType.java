/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.runtime.logging;

public final class GCLoggingMemoryEventType {
  public static GCLoggingMemoryEventType LONG_GC = new GCLoggingMemoryEventType("LONG_GC");

  private String                       name;

  public GCLoggingMemoryEventType(String name) {
    this.name = name;
  }

  public String toString() {
    return "GCLoggingMemoryEventType." + name;
  }
}
