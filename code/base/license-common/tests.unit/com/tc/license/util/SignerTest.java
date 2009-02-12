/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import com.tc.license.License;

import junit.framework.TestCase;

public class SignerTest extends TestCase {

  public void testAlteredLicense1() throws Exception {
    LicenseFormat licenseStore = new TerracottaLicenseFormat();
    License license = licenseStore.load(getClass().getResourceAsStream("altered-license1.txt"));
    Signer signer = new TerracottaSigner();
    assertFalse(signer.verify(license.getCanonicalData(), license.getSignature()));
  }

  public void testAlteredLicense2() throws Exception {
    LicenseFormat licenseStore = new TerracottaLicenseFormat();
    License license = licenseStore.load(getClass().getResourceAsStream("altered-license2.txt"));
    Signer signer = new TerracottaSigner();
    assertFalse(signer.verify(license.getCanonicalData(), license.getSignature()));
  }
  
  public void testValidLicense() throws Exception {
    LicenseFormat licenseStore = new TerracottaLicenseFormat();
    License license = licenseStore.load(getClass().getResourceAsStream("fx.txt"));
    Signer signer = new TerracottaSigner();
    assertTrue(signer.verify(license.getCanonicalData(), license.getSignature()));
  }
}
