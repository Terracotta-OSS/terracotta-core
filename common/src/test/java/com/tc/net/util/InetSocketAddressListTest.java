/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.util;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.net.InetSocketAddress;

public final class InetSocketAddressListTest extends TCTestCase {

  private InetSocketAddress[]   addresses;
  private InetSocketAddressList list;

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
