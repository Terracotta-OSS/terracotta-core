/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import com.tc.license.Capability;
import com.tc.license.License;
import com.tc.license.LicenseFactory;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class LicenseFactoryTest extends TestCase {

  public void testFactory() throws Exception {
    Map<String, String> rawValueFields = new HashMap<String, String>();
    rawValueFields.put(LicenseConstants.LICENSE_TYPE, "Commercial");
    rawValueFields.put(LicenseConstants.LICENSE_NUMBER, "123");
    rawValueFields.put(LicenseConstants.LICENSEE, "Big ole company");
    rawValueFields.put(LicenseConstants.MAX_CLIENTS, "4");
    rawValueFields.put(LicenseConstants.PRODUCT, "Ehcache");
    rawValueFields.put(LicenseConstants.EDITION, "EX");
    rawValueFields.put(LicenseConstants.EXPIRATION_DATE, null);
    
    License license = LicenseFactory.createEnterpriseLicense(rawValueFields);
    assertEquals("Commercial", license.licenseType());
    assertEquals("123", license.licenseNumber());
    assertEquals("Big ole company", license.licensee());
    assertEquals("Ehcache", license.product());
    assertEquals("EX", license.edition());
    assertEquals(4, license.maxClients());
    assertTrue(license.capabilities().isLicensed(new Capability("Terracotta operator console")));
    assertTrue(license.capabilities().isLicensed(new Capability("authentication")));
    assertTrue(license.capabilities().isLicensed(new Capability("ehcache")));
    assertNull(license.expirationDate());
  }

  public void testFactoryValidator() throws Exception {
    try {
      Map<String, String> rawValueFields = new HashMap<String, String>();
      rawValueFields.put(LicenseConstants.LICENSE_TYPE, "BOGUS");
      rawValueFields.put(LicenseConstants.LICENSE_NUMBER, "123");
      rawValueFields.put(LicenseConstants.LICENSEE, "Big ole company");
      rawValueFields.put(LicenseConstants.MAX_CLIENTS, "4");
      rawValueFields.put(LicenseConstants.PRODUCT, "Ehcache");
      rawValueFields.put(LicenseConstants.EDITION, "EX");
      rawValueFields.put(LicenseConstants.EXPIRATION_DATE, null);
      
      LicenseFactory.createEnterpriseLicense(rawValueFields);
      
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      // expected
    }
  }

}
