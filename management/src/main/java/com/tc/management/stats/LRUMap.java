/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUMap extends LinkedHashMap {
  private final static int NO_LIMIT = -1;

  private final int maxSize;

  public LRUMap() {
    this(NO_LIMIT);
  }

  public LRUMap(int maxSize) {
    super(100, 0.75f, true);
    this.maxSize = maxSize;
  }

  protected boolean removeEldestEntry(Map.Entry eldest) {
    if (maxSize != NO_LIMIT) {
      return size() >= this.maxSize;
    } else {
      return false;
    }
  }
}
