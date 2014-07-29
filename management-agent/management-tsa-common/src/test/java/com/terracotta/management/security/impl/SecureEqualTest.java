/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.security.impl;

import org.junit.Assert;
import org.junit.Test;

public class SecureEqualTest {

  @Test
  public void testSecureEqual() {
    String a = "stringa";
    String b = "stringb";

    Assert.assertTrue(DfltRequestTicketMonitor.isEqualSecure(a, "stringa"));
    Assert.assertTrue(DfltRequestTicketMonitor.isEqualSecure(b.getBytes(), new byte[] { 's', 't', 'r', 'i', 'n', 'g',
        'b' }));
    Assert.assertFalse(DfltRequestTicketMonitor.isEqualSecure(a, b));
  }
}
