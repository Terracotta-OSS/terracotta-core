/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.async.impl;

import org.slf4j.LoggerFactory;

import com.tc.async.api.OrderedEventContext;
import com.tc.async.api.Sink;
import com.tc.test.TCTestCase;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class OrderedSinkTest extends TCTestCase {

  public void testBasic() throws Exception {
    MockSink<OrderedEventContext> des = new MockSink<OrderedEventContext>();
    Sink<OrderedEventContext> s = new OrderedSink(LoggerFactory.getLogger(OrderedSink.class), des);

    OrderedEventContext oc = new MyOrderedEventContext(1);
    s.addToSink(oc);
    assertEvents(des, 1, 1);

    oc = new MyOrderedEventContext(3);
    s.addToSink(oc);
    assertTrue(des.queue.isEmpty());

    oc = new MyOrderedEventContext(4);
    s.addToSink(oc);
    assertTrue(des.queue.isEmpty());

    oc = new MyOrderedEventContext(2);
    s.addToSink(oc);
    assertEvents(des, 2, 3);

    oc = new MyOrderedEventContext(2);
    boolean failed = false;
    try {
      s.addToSink(oc);
      failed = true;
    } catch (AssertionError ae) {
      // Excepted
    }
    assertFalse(failed);
  }

  public void testComplex() throws Exception {
    MockSink<OrderedEventContext> des = new MockSink<OrderedEventContext>();
    Sink<OrderedEventContext> s = new OrderedSink(LoggerFactory.getLogger(OrderedSink.class), des);
    
    List<MyOrderedEventContext> l = createOrderedEvents(1000);
    SecureRandom r = new SecureRandom();

    while (!l.isEmpty()) {
      int idx = r.nextInt(l.size());
      MyOrderedEventContext oc = l.remove(idx);
      s.addToSink(oc);
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
