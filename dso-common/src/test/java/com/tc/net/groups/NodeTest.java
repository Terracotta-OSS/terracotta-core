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
