/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import junit.framework.TestCase;

public class ConnectionAddressIteratorTest extends TestCase {

  public final void testEmpty() {
    ConnectionAddressIterator i = new ConnectionAddressIterator(new ConnectionInfo[0]);
    assertFalse(i.hasNext());
    assertNull(i.next());
  }
  
  public final void testOne() {
    final ConnectionInfo[] cis = new ConnectionInfo[] { new ConnectionInfo("1", 1) };
    ConnectionAddressIterator i = new ConnectionAddressIterator(cis);
    assertTrue(i.hasNext());
    assertTrue(i.hasNext()); // multi calls to hasNext should not change state
    assertSame(cis[0], i.next());
    assertFalse(i.hasNext());
    assertFalse(i.hasNext());
    assertNull(i.next());
  }
  public final void testMany() {
    final ConnectionInfo[] cis = new ConnectionInfo[] { new ConnectionInfo("1", 1), new ConnectionInfo("2", 2), new ConnectionInfo("3", 3) };
    ConnectionAddressIterator iter = new ConnectionAddressIterator(cis);
    for (int i = 0; i < cis.length; i++) {
      assertTrue(iter.hasNext());
      assertSame(cis[i], iter.next());
    }
    assertFalse(iter.hasNext());
    assertNull(iter.next());
  }
}
