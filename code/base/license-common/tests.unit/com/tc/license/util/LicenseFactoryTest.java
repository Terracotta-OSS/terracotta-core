/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import com.tc.license.License;
import com.tc.license.LicenseFactory;

import junit.framework.TestCase;

public class LicenseFactoryTest extends TestCase {

  public void testFactory() throws Exception {
    License license = LicenseFactory.createEnterpriseLicense("Commercial", "123", "Big ole company", "EX", "4", null);
    assertEquals("Commercial", license.licenseType());
    assertEquals("123", license.licenseNumber());
    assertEquals("Big ole company", license.licensee());
    assertEquals("EX", license.product());
    assertEquals(4, license.maxClients());
    assertTrue(license.capabilities().allowRoots());
    assertTrue(license.capabilities().allowSessions());
    assertNull(license.expirationDate());
  }

  public void testFactoryValidator() throws Exception {
    try {
      LicenseFactory.createEnterpriseLicense("Unknown", "123", "Big ole company", "EX", "4", null);
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      // expected
    }
  }

}
