package com.tc.license;

import com.tc.license.util.LicenseConstants;

import java.util.Arrays;
import java.util.EnumSet;

public enum Capability {
  ROOTS(LicenseConstants.ROOTS), SESSIONS(LicenseConstants.SESSIONS), TOC(LicenseConstants.TOC), SERVER_STRIPING(
      LicenseConstants.SERVER_STRIPING);

  private final String name;

  private Capability(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }

  public static Capability parse(String name) {
    if (LicenseConstants.ROOTS.equals(name)) return ROOTS;
    if (LicenseConstants.SESSIONS.equals(name)) return SESSIONS;
    if (LicenseConstants.TOC.equals(name)) return TOC;
    if (LicenseConstants.SERVER_STRIPING.equals(name)) return SERVER_STRIPING;
    throw new IllegalArgumentException("Capability unknown: " + name + ". Valid: " + Arrays.asList(Capability.values()));
  }

  public static EnumSet<Capability> toSet(String capabilities) {
    EnumSet<Capability> set = EnumSet.noneOf(Capability.class);
    String[] tokens = capabilities.split(",");
    for (String t : tokens) {
      set.add(Capability.parse(t.trim()));
    }
    return set;
  }

  public static String convertToString(EnumSet<Capability> set) {
    StringBuffer sb = new StringBuffer();
    int index = 0;
    for (Capability c : set) {
      if (index > 0) {
        sb.append(", ");
      }
      sb.append(c.toString());
      index++;
    }
    return sb.toString();
  }
}
