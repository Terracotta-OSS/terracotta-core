/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.cache;


public class ClockCacheTest extends LRUEvictionPolicyTest {
  public EvictionPolicy createNewCache(int size) {
    return new ClockEvictionPolicy(size);
  }
}
