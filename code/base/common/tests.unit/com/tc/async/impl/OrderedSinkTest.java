/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.OrderedEventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogging;
import com.tc.test.TCTestCase;

import java.security.SecureRandom;
import java.util.ArrayList;

public class OrderedSinkTest extends TCTestCase {

  public void testBasic() throws Exception {
    MockSink des = new MockSink();
    Sink s = new OrderedSink(TCLogging.getLogger(OrderedSink.class), des);

    OrderedEventContext oc = new MyOrderedEventContext(1);
    s.add(oc);
    assertEvents(des, 1, 1);

    oc = new MyOrderedEventContext(3);
    s.add(oc);
    assertTrue(des.queue.isEmpty());

    oc = new MyOrderedEventContext(4);
    s.add(oc);
    assertTrue(des.queue.isEmpty());

    oc = new MyOrderedEventContext(2);
    s.add(oc);
    assertEvents(des, 2, 3);

    oc = new MyOrderedEventContext(2);
    boolean failed = false;
    try {
      s.add(oc);
      failed = true;
    } catch (AssertionError ae) {
      // Excepted
    }
    assertFalse(failed);
  }

  public void testComplex() throws Exception {
    MockSink des = new MockSink();
    Sink s = new OrderedSink(TCLogging.getLogger(OrderedSink.class), des);
    
    ArrayList l = createOrderedEvents(1000);
    SecureRandom r = new SecureRandom();

    while (!l.isEmpty()) {
      int idx = r.nextInt(l.size());
      OrderedEventContext oc = (OrderedEventContext) l.remove(idx);
      s.add(oc);
    }
    
    assertEvents(des, 1, 1000);
  }

  private ArrayList createOrderedEvents(int count) {
    ArrayList al = new ArrayList(count);
    for (int i = 1; i <= count; i++) {
      al.add(new MyOrderedEventContext(i));
    }
    return al;
  }

  private void assertEvents(MockSink des, int id, int count) {
    while (count-- > 0) {
      MyOrderedEventContext oc;
      try {
        oc = (MyOrderedEventContext) des.queue.take();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      assertEquals(id++, oc.getSequenceID());
    }
    assertTrue(des.queue.isEmpty());
  }

  private static class MyOrderedEventContext implements OrderedEventContext {

    long id;

    public MyOrderedEventContext(int i) {
      id = i;
    }

    public long getSequenceID() {
      return id;
    }

  }
}
