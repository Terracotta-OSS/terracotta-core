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
package com.tc.net.util;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.net.InetSocketAddress;

public final class InetSocketAddressListTest extends TCTestCase {

  private InetSocketAddress[]   addresses;
  private InetSocketAddressList list;

  @Override
  protected void setUp() throws Exception {
    addresses = new InetSocketAddress[] { new InetSocketAddress("localhost", 0),
        new InetSocketAddress("www.terracottatech.com", 80), new InetSocketAddress("15.0.0.1", 5) };
    list = new InetSocketAddressList(addresses);
    super.setUp();
  }

  public final void testConstructor() throws Exception {
    new InetSocketAddressList(new InetSocketAddress[0]);
    new InetSocketAddressList(addresses);
    try {
      new InetSocketAddressList(null);
    } catch (NullPointerException npe) {
      // Expected
    }
    try {
      new InetSocketAddressList(new InetSocketAddress[] { new InetSocketAddress("localhost", 0), null,
          new InetSocketAddress("www.terracottatech.com", 80) });
    } catch (NullPointerException npe) {
      // Expected
    }
  }

  public final void testToString() throws Exception {
    String toString = list.toString();
    assertTrue(toString.matches("^(?:[^:]+:\\p{Digit}+)(?:,[^:]+:\\p{Digit}+){2}$"));
  }

  public final void testParseAddresses() throws Exception {
    InetSocketAddress[] fromStringAddresses = InetSocketAddressList.parseAddresses(list.toString());
    assertEquals(addresses.length, fromStringAddresses.length);
    for (int pos = 0; pos < fromStringAddresses.length; pos++) {
      Assert.assertEquals(addresses[pos], fromStringAddresses[pos]);
    }
  }

}
