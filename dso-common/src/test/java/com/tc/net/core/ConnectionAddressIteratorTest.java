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
