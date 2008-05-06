/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.test.EqualityChecker;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link LicenseType}.
 */
public class LicenseTypeTest extends TCTestCase {

  public void testAll() throws Exception {
    Object[] arr1 = new Object[] { LicenseType.TRIAL, LicenseType.PRODUCTION, LicenseType.NONE };
    Object[] arr2 = new Object[] { LicenseType.TRIAL, LicenseType.PRODUCTION, LicenseType.NONE };

    EqualityChecker.checkArraysForEquality(arr1, arr2);

    assertFalse(LicenseType.TRIAL.equals(null));
    assertFalse(LicenseType.TRIAL.equals("trial"));
    assertFalse(LicenseType.TRIAL.equals("foo"));

    LicenseType.TRIAL.toString();
    LicenseType.PRODUCTION.toString();
    LicenseType.NONE.toString();
  }

}
