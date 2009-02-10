package com.tc.license.util;

import com.tc.license.Capability;

import java.util.EnumSet;
import java.util.Map;

import junit.framework.TestCase;

public class LicenseDescriptorTest extends TestCase {
  
  public void testLicensedCapabilities() {
    LicenseDescriptor d = new LicenseDescriptor();
    EnumSet<Capability> capabilities = d.getLicensedCapabilities(LicenseConstants.EX);
    assertEquals(2, capabilities.size());
    assertTrue(capabilities.contains(Capability.ROOTS));
    assertTrue(capabilities.contains(Capability.SESSIONS));

    capabilities = d.getLicensedCapabilities(LicenseConstants.EX_SESSIONS);
    assertEquals(1, capabilities.size());
    assertTrue(capabilities.contains(Capability.SESSIONS));

    capabilities = d.getLicensedCapabilities(LicenseConstants.FX);
    assertEquals(4, capabilities.size());
    assertTrue(capabilities.contains(Capability.ROOTS));
    assertTrue(capabilities.contains(Capability.SESSIONS));
    assertTrue(capabilities.contains(Capability.TOC));
    assertTrue(capabilities.contains(Capability.SERVER_STRIPING));

    capabilities = d.getLicensedCapabilities(LicenseConstants.ES);
    assertEquals(2, capabilities.size());
    assertTrue(capabilities.contains(Capability.ROOTS));
    assertTrue(capabilities.contains(Capability.SESSIONS));
  }

  public void testFields() {
    LicenseDescriptor d = new LicenseDescriptor();
    Map description = d.getDescriptionMap();
    assertEquals(6, description.size());
    assertNotNull(description.get(LicenseConstants.LICENSE_TYPE));
    assertNotNull(description.get(LicenseConstants.LICENSE_NUMBER));
    assertNotNull(description.get(LicenseConstants.LICENSEE));
    assertNotNull(description.get(LicenseConstants.MAX_CLIENTS));
    assertNotNull(description.get(LicenseConstants.PRODUCT));
    assertNotNull(description.get(LicenseConstants.EXPIRATION_DATE));
  }
}
