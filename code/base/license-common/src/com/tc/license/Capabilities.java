/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license;

import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Set;

public final class Capabilities {
  private final Set<Capability> licensedCapabilities;
  private final Set<Capability> supportedCapabilities;

  public Capabilities(Set<Capability> licensedCapabilities, Set<Capability> supportedCapabilities) {
    this.licensedCapabilities = new HashSet(licensedCapabilities);
    this.supportedCapabilities = new HashSet(supportedCapabilities);
    if (!this.supportedCapabilities.containsAll(this.licensedCapabilities)) {
      //
      throw new AssertionError("Licensed capabilities have to be a subset of supported capabilities");
    }
  }

  public boolean isSupported(Capability capability) {
    return supportedCapabilities.contains(capability);
  }

  public boolean isLicensed(Capability capability) {
    return licensedCapabilities.contains(capability);
  }

  public int licensedCapabilitiesCount() {
    return licensedCapabilities.size();
  }

  public String getLicensedCapabilitiesAsString() {
    return StringUtils.join(licensedCapabilities.iterator(), ", ");
  }
}
