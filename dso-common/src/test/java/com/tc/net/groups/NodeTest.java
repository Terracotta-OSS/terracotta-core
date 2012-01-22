/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import junit.framework.TestCase;

public class NodeTest extends TestCase {

  public void testCtor() {
    try {
      new Node(null, 1);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // ignore
    }
    try {
      new Node("  ", 1);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // ignore
    }
    try {
      new Node("host", -1);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // ignore
    }

    final String host = "host";
    final int port = 123;
    final Node n = new Node(host, port);
    assertEquals(host, n.getHost());
    assertEquals(port, n.getPort());
  }

  public void testEqualsAndHashCode() {
    final String host = "host";
    final int port = 123;
    final Node n1 = new Node(host, port);
    final Node n2 = new Node(host, port);
    assertEquals(n1, n2);
    assertEquals(n1, new Node(" " + host + " ", port));

    assertFalse(n1.equals(new Node(host + "1", port)));
    assertFalse(n1.equals(new Node(host, port + 1)));

    final Node n3 = new Node(host, port);
    final Node n4 = new Node(host, port);
    assertEquals(n3, n4); // differing bind should not influence equals() or hashCode()
    assertEquals(n3.hashCode(), n4.hashCode());
  }
}
