package com.tc.license.util;

import com.tc.license.Capability;

import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class LicenseDescriptorTest extends TestCase {

  public void testLicensedCapabilities() {
    testProduct("Enterprise Suite", "EX", 6,
                "roots, Terracotta operator console, authentication, ehcache, quartz, sessions");
    testProduct("Enterprise Suite", "FX", 8,
                "roots, Terracotta operator console, authentication, ehcache, quartz, sessions, DCV2, server striping");
    testProduct("Ehcache", "EX", 3, "Terracotta operator console, authentication, ehcache");
    testProduct("Ehcache", "FX", 5, "Terracotta operator console, server striping, DCV2, authentication, ehcache");
    testProduct("Quartz", "EX", 3, "Terracotta operator console, authentication, quartz");
    testProduct("Quartz", "FX", 5, "Terracotta operator console, server striping, DCV2, authentication, quartz");
    testProduct("Sessions", "EX", 3, "Terracotta operator console, authentication, sessions");
    testProduct("Sessions", "FX", 5, "Terracotta operator console, server striping, DCV2, authentication, sessions");
  }

  private void testProduct(String product, String edition, int capabilitiesCount, String expectedCapabilities) {
    LicenseDescriptor d = new LicenseDescriptor();
    Set<Capability> capabilities = d.getLicensedCapabilities(product, edition);
    assertCapabilities(capabilitiesCount, capabilities, expectedCapabilities);
  }

  private void assertCapabilities(int capabilitiesCount, Set<Capability> capabilities, String expectedCapabilities) {
    assertEquals(capabilitiesCount, capabilities.size());
    String[] tokens = expectedCapabilities.split("\\s*,\\s*");
    for (String token : tokens) {
      assertTrue(capabilities.contains(new Capability(token)));
    }
  }

  public void testFields() {
    LicenseDescriptor d = new LicenseDescriptor();
    Map description = d.getDescriptionMap();
    assertEquals(7, description.size());
    assertNotNull(description.get(LicenseConstants.LICENSE_TYPE));
    assertNotNull(description.get(LicenseConstants.LICENSE_NUMBER));
    assertNotNull(description.get(LicenseConstants.LICENSEE));
    assertNotNull(description.get(LicenseConstants.MAX_CLIENTS));
    assertNotNull(description.get(LicenseConstants.PRODUCT));
    assertNotNull(description.get(LicenseConstants.EDITION));
    assertNotNull(description.get(LicenseConstants.EXPIRATION_DATE));
  }
}
