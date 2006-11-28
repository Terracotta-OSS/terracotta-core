/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.runtime;

public final class MemoryEventType {

  public static final MemoryEventType ABOVE_THRESHOLD          = new MemoryEventType("ABOVE_THRESHOLD");
  public static final MemoryEventType ABOVE_CRITICAL_THRESHOLD = new MemoryEventType("ABOVE_CRITICAL_THRESHOLD");
  public static final MemoryEventType BELOW_THRESHOLD          = new MemoryEventType("BELOW_THRESHOLD");

  private final String                name;

  // Not exposed to anyone
  private MemoryEventType(String name) {
    this.name = name;
  }

  public String toString() {
    return "MemoryEventType." + name;
  }

}
