package com.tc.license.util;

import com.tc.license.Capabilities;

import java.util.Map;

import junit.framework.TestCase;

public class LicenseDescriptorTest extends TestCase {
  public void testSupportedCapabilities() {
    LicenseDescriptor d = LicenseDescriptor.getInstance();
    Capabilities capabilities = d.getCapabilities(LicenseConstants.EX);
    assertEquals(2, capabilities.size());
    assertTrue(capabilities.allowRoots());
    assertTrue(capabilities.allowSessions());

    capabilities = d.getCapabilities(LicenseConstants.EX_SESSIONS);
    assertEquals(1, capabilities.size());
    assertTrue(capabilities.allowSessions());

    capabilities = d.getCapabilities(LicenseConstants.FX);
    assertEquals(4, capabilities.size());
    assertTrue(capabilities.allowRoots());
    assertTrue(capabilities.allowSessions());
    assertTrue(capabilities.allowTOC());
    assertTrue(capabilities.allowServerStripping());

    capabilities = d.getCapabilities(LicenseConstants.ES);
    assertEquals(2, capabilities.size());
    assertTrue(capabilities.allowRoots());
    assertTrue(capabilities.allowSessions());
  }

  public void testFields() {
    LicenseDescriptor d = LicenseDescriptor.getInstance();
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
