/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
