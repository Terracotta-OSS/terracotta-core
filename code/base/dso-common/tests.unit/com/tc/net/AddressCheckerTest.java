/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net;

import java.net.InetAddress;

import junit.framework.TestCase;

public class AddressCheckerTest extends TestCase {

  public void test() throws Exception {
    AddressChecker ac = new AddressChecker();

    assertTrue(ac.isLegalBindAddress(InetAddress.getByName("127.0.0.1")));
    assertTrue(ac.isLegalBindAddress(InetAddress.getByName("0.0.0.0")));
    assertTrue(ac.isLegalBindAddress(InetAddress.getLocalHost()));

    assertFalse(ac.isLegalBindAddress(InetAddress.getByName("www.sun.com")));
  }

}
