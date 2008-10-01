/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime.cache;

public final class CacheMemoryEventType {

  public static final CacheMemoryEventType ABOVE_THRESHOLD          = new CacheMemoryEventType("ABOVE_THRESHOLD");
  public static final CacheMemoryEventType ABOVE_CRITICAL_THRESHOLD = new CacheMemoryEventType("ABOVE_CRITICAL_THRESHOLD");
  public static final CacheMemoryEventType BELOW_THRESHOLD          = new CacheMemoryEventType("BELOW_THRESHOLD");

  private final String                name;

  // Not exposed to anyone
  private CacheMemoryEventType(String name) {
    this.name = name;
  }

  public String toString() {
    return "CacheMemoryEventType." + name;
  }

}
