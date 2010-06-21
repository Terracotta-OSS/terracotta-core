/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import junit.framework.TestCase;

public class SignerTest extends TestCase {

  public void testAlteredLicense1() throws Exception {
    LicenseFormat licenseStore = new TerracottaLicenseFormat();
    try {
      licenseStore.loadAndVerify(getClass().getResourceAsStream("altered-license1.txt"), new TerracottaSigner());
      fail("Should have failed");
    } catch (LicenseException e) {
      // expected;
    }
  }

  public void testAlteredLicense2() throws Exception {
    LicenseFormat licenseStore = new TerracottaLicenseFormat();
    try {
      licenseStore.loadAndVerify(getClass().getResourceAsStream("altered-license2.txt"), new TerracottaSigner());
      fail("Should have failed");
    } catch (LicenseException e) {
      // expected;
    }
  }

  public void testValidLicense() throws Exception {
    LicenseFormat licenseStore = new TerracottaLicenseFormat();
    licenseStore.loadAndVerify(getClass().getResourceAsStream("fx.txt"), new TerracottaSigner());
  }
}
