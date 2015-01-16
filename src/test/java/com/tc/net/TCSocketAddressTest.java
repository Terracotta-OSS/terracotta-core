/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TCSocketAddressTest {

  @Test
  public void testHashCode() {
    TCSocketAddress tsa1 = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 9000);
    TCSocketAddress tsa2 = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 9000);

    assertTrue(tsa1.hashCode() == tsa2.hashCode());
  }

  @SuppressWarnings("unused")
  @Test
  public void testPortRanges() {
    try {
      new TCSocketAddress(-1);
      fail();
    } catch (IllegalArgumentException e) {
      //expected
    }

    try {
      new TCSocketAddress(65536);
      fail();
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testStringForm() {
    TCSocketAddress sa = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 12345);
    String s = sa.getStringForm();
    assertTrue(s.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+\\:12345$"));
  }

  @Test
  public void testEquals() {
    TCSocketAddress tsa1 = new TCSocketAddress(9000);
    TCSocketAddress tsa2 = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 9000);
    TCSocketAddress tsa3 = new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 9000);

    assertTrue(tsa1.equals(tsa2));
    assertTrue(tsa2.equals(tsa3));
  }

  @Test
  public void testInCollections() throws UnknownHostException {
    TCSocketAddress tsa1 = new TCSocketAddress(9000);
    TCSocketAddress tsa2 = new TCSocketAddress(InetAddress.getByName("1.2.3.4"), 9000);
    TCSocketAddress tsa3 = new TCSocketAddress(InetAddress.getByName("1.2.3.4"), 9000);

    Map<TCSocketAddress, Object> map = new HashMap<TCSocketAddress, Object>();

    Object val1 = new Object();
    Object val2 = new Object();
    Object val3 = new Object();

    map.put(tsa1, val1);
    map.put(tsa2, val2);
    map.put(tsa3, val3);

    assertTrue(map.size() == 2);

    assertTrue(map.get(tsa1) == val1);
    assertTrue(map.get(tsa2) == val3);
    assertTrue(map.get(tsa3) == val3);
  }
}
