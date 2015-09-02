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
import java.util.List;

public class OrderedSinkTest extends TCTestCase {

  public void testBasic() throws Exception {
    MockSink<OrderedEventContext> des = new MockSink<OrderedEventContext>();
    Sink<OrderedEventContext> s = new OrderedSink(TCLogging.getLogger(OrderedSink.class), des);

    OrderedEventContext oc = new MyOrderedEventContext(1);
    s.addSingleThreaded(oc);
    assertEvents(des, 1, 1);

    oc = new MyOrderedEventContext(3);
    s.addSingleThreaded(oc);
    assertTrue(des.queue.isEmpty());

    oc = new MyOrderedEventContext(4);
    s.addSingleThreaded(oc);
    assertTrue(des.queue.isEmpty());

    oc = new MyOrderedEventContext(2);
    s.addSingleThreaded(oc);
    assertEvents(des, 2, 3);

    oc = new MyOrderedEventContext(2);
    boolean failed = false;
    try {
      s.addSingleThreaded(oc);
      failed = true;
    } catch (AssertionError ae) {
      // Excepted
    }
    assertFalse(failed);
  }

  public void testComplex() throws Exception {
    MockSink<OrderedEventContext> des = new MockSink<OrderedEventContext>();
    Sink<OrderedEventContext> s = new OrderedSink(TCLogging.getLogger(OrderedSink.class), des);
    
    List<MyOrderedEventContext> l = createOrderedEvents(1000);
    SecureRandom r = new SecureRandom();

    while (!l.isEmpty()) {
      int idx = r.nextInt(l.size());
      MyOrderedEventContext oc = l.remove(idx);
      s.addSingleThreaded(oc);
    }
    
    assertEvents(des, 1, 1000);
  }

  private List<MyOrderedEventContext> createOrderedEvents(int count) {
    List<MyOrderedEventContext> al = new ArrayList<MyOrderedEventContext>(count);
    for (int i = 1; i <= count; i++) {
      al.add(new MyOrderedEventContext(i));
    }
    return al;
  }

  private void assertEvents(MockSink<OrderedEventContext> des, int id, int count) {
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

    @Override
    public long getSequenceID() {
      return id;
    }

  }
}
