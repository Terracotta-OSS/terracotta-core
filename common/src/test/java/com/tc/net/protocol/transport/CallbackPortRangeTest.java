/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

public class CallbackPortRangeTest extends TestCase {

  public void testRangeParse() {
    Set<Integer> ports = CallbackPortRange.expandRange("");
    assertEquals(0, ports.size());

    ports = CallbackPortRange.expandRange("  ");
    assertEquals(0, ports.size());

    ports = CallbackPortRange.expandRange("1,4,3,2");
    Iterator<Integer> iter = ports.iterator();
    assertEquals(4, ports.size());
    assertEquals(1, iter.next().intValue());
    assertEquals(4, iter.next().intValue());
    assertEquals(3, iter.next().intValue());
    assertEquals(2, iter.next().intValue());

    ports = CallbackPortRange.expandRange("1-4");
    assertEquals(4, ports.size());

    ports = CallbackPortRange.expandRange("  1, 3  - 4,2 ,  5  ");
    iter = ports.iterator();
    assertEquals(5, ports.size());
    assertEquals(1, iter.next().intValue());
    assertEquals(3, iter.next().intValue());
    assertEquals(4, iter.next().intValue());
    assertEquals(2, iter.next().intValue());
    assertEquals(5, iter.next().intValue());

    ports = CallbackPortRange.expandRange("3-4,2-4");
    iter = ports.iterator();
    assertEquals(3, ports.size());
    assertEquals(3, iter.next().intValue());
    assertEquals(4, iter.next().intValue());
    assertEquals(2, iter.next().intValue());

    ports = CallbackPortRange.expandRange("5-5");
    assertEquals(1, ports.size());
    assertEquals(5, ports.iterator().next().intValue());

    try {
      CallbackPortRange.expandRange("4-1");
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      CallbackPortRange.expandRange("wazzup");
      fail();
    } catch (NumberFormatException nfe) {
      // expected
    }

    ports = CallbackPortRange.expandRange(String.valueOf(TransportHandshakeMessage.NO_CALLBACK_PORT));
    assertEquals(1, ports.size());
    assertEquals(TransportHandshakeMessage.NO_CALLBACK_PORT, ports.iterator().next().intValue());
  }

}
