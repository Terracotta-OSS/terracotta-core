/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license;

import java.util.EnumSet;

public final class Capabilities {
  private final EnumSet<Capability> licensedCapabilities;
  private final EnumSet<Capability> supportedCapabilities;

  public Capabilities(EnumSet<Capability> licensedCapabilities, EnumSet<Capability> supportedCapabilities) {
    this.licensedCapabilities = licensedCapabilities.clone();
    this.supportedCapabilities = supportedCapabilities.clone();
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
    return Capability.convertToString(licensedCapabilities);
  }
}
