/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.counter;

import junit.framework.TestCase;

public class BoundedCounterTest extends TestCase {
  
  public void testBasic() throws Exception {
    CounterManager counterManager = new CounterManagerImpl();
    Counter c = counterManager.createCounter(new BoundedCounterConfig(0L, -100, 100));
    
    assertEquals(0, c.getValue());
    assertEquals(50, c.increment(50L));
    assertEquals(100, c.increment(50L));
    assertEquals(100, c.increment(50L));
    assertEquals(100, c.increment());
    assertEquals(99, c.decrement());
    assertEquals(100, c.increment());
    assertEquals(100, c.increment());
    assertEquals(100, c.getValue());
    assertEquals(50, c.decrement(50L));
    assertEquals(0, c.decrement(50L));
    assertEquals(-100, c.decrement(110));
    assertEquals(-100, c.decrement());
    assertEquals(-100, c.getAndSet(50));
    assertEquals(50, c.getAndSet(100));
    assertEquals(100, c.getAndSet(200));
    assertEquals(100, c.getAndSet(200));
    c.setValue(0);
    assertEquals(0, c.getValue());
    c.setValue(-3000);
    assertEquals(-100, c.getValue());
    c.setValue(100);
    assertEquals(100, c.getValue());
    c.setValue(2000);
    assertEquals(100, c.getAndSet(77));
    assertEquals(77, c.getValue());
  }

}
