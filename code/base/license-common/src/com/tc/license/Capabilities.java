/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license;

import java.util.EnumSet;

public class Capabilities {
  private final EnumSet<Capability> capabilities;

  public Capabilities(String capabilities) {
    this.capabilities = Capability.toSet(capabilities);
  }

  public Capabilities(EnumSet<Capability> set) {
    this.capabilities = set;
  }

  public boolean allowRoots() {
    return capabilities.contains(Capability.ROOTS);
  }

  public boolean allowSessions() {
    return capabilities.contains(Capability.SESSIONS);
  }

  public boolean allowTOC() {
    return capabilities.contains(Capability.TOC);
  }

  public boolean allowServerStripping() {
    return capabilities.contains(Capability.SERVER_STRIPING);
  }

  public int size() {
    return capabilities.size();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    int index = 0;
    for (Capability c : capabilities) {
      if (index > 0) {
        sb.append(", ");
      }
      sb.append(c.toString());
      index++;
    }
    return sb.toString();
  }
}
