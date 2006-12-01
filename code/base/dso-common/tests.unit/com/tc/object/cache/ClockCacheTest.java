/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;


public class ClockCacheTest extends LRUEvictionPolicyTest {
  public EvictionPolicy createNewCache(int size) {
    return new ClockEvictionPolicy(size);
  }
}
