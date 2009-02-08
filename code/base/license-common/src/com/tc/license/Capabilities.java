/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license;

import java.util.EnumSet;

public final class Capabilities {
  private final EnumSet<Capability> licensedCapabilities;
  private final EnumSet<Capability> supportedCapabilities;

  public Capabilities(String licensedCapabilities, String supportedCapabilities) {
    this.licensedCapabilities = Capability.toSet(licensedCapabilities);
    this.supportedCapabilities = Capability.toSet(supportedCapabilities);
  }

  public Capabilities(EnumSet<Capability> licensedCapabilities, EnumSet<Capability> supportedCapabilities) {
    this.licensedCapabilities = licensedCapabilities;
    this.supportedCapabilities = supportedCapabilities;
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

  public String toString() {
    StringBuffer sb = new StringBuffer();
    int index = 0;
    for (Capability c : licensedCapabilities) {
      if (index > 0) {
        sb.append(", ");
      }
      sb.append(c.toString());
      index++;
    }
    return sb.toString();
  }
}
