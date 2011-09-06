/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.test.EqualityChecker;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link L2ConfigForL1}.
 */
public class L2ConfigForL1Test extends TCTestCase {

  public void testL2Data() throws Exception {
    try {
      new L2ConfigForL1.L2Data(null, 20);
      fail("Didn't get NPE on no host");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new L2ConfigForL1.L2Data("", 20);
      fail("Didn't get IAE on empty host");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      new L2ConfigForL1.L2Data("   ", 20);
      fail("Didn't get IAE on blank host");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    L2ConfigForL1.L2Data config = new L2ConfigForL1.L2Data("foobar", 20);
    assertEquals("foobar", config.host());
    assertEquals(20, config.dsoPort());

    EqualityChecker.checkArraysForEquality(
        new Object[] {
                      new L2ConfigForL1.L2Data("foobar", 20),
                      new L2ConfigForL1.L2Data("foobaz", 20),
                      new L2ConfigForL1.L2Data("foobar", 2),
                      new L2ConfigForL1.L2Data("foobar", 30) },
        new Object[] {
                      new L2ConfigForL1.L2Data("foobar", 20),
                      new L2ConfigForL1.L2Data("foobaz", 20),
                      new L2ConfigForL1.L2Data("foobar", 2),
                      new L2ConfigForL1.L2Data("foobar", 30) });
  }

}
