/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import junit.framework.TestCase;

public class NodeTest extends TestCase {

  public void testCtor() {
    try {
      new Node(null, 1, null);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // ignore
    }
    try {
      new Node("  ", 1, null);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // ignore
    }
    try {
      new Node("host", -1, null);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // ignore
    }

    final String host = "host";
    final int port = 123;
    final Node n = new Node(host, port, null);
    assertEquals(host, n.getHost());
    assertEquals(port, n.getPort());
  }

  public void testEqualsAndHashCode() {
    final String host = "host";
    final int port = 123;
    final Node n1 = new Node(host, port, null);
    final Node n2 = new Node(host, port, null);
    assertEquals(n1, n2);
    assertEquals(n1, new Node(" " + host + " ", port, null));

    assertFalse(n1.equals(new Node(host + "1", port, null)));
    assertFalse(n1.equals(new Node(host, port + 1, null)));

    final Node n3 = new Node(host, port, "127.0.0.1");
    final Node n4 = new Node(host, port, "0.0.0.0");
    assertEquals(n3, n4); // differing bind should not influence equals() or hashCode()
    assertEquals(n3.hashCode(), n4.hashCode());
  }
}
