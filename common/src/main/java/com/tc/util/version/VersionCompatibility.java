/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.version;

public class VersionCompatibility {
  protected static final Version MINIMUM_COMPATIBLE_PERSISTENCE = new Version("4.1.2");

  public boolean isCompatibleClientServer(Version clientVersion, Version serverVersion) {
    return isCompatible(clientVersion, serverVersion);
  }

  public boolean isCompatibleServerServer(Version v1, Version v2) {
    return isCompatible(v1, v2);
  }

  public boolean isCompatibleServerPersistence(Version persisted, Version current) {
    if (persisted.major() == current.major() && persisted.minor() == current.minor()) {
      return true;
    } else {
      return current.compareTo(persisted) >= 0 && MINIMUM_COMPATIBLE_PERSISTENCE.compareTo(persisted) <= 0;
    }
  }

  private static boolean isCompatible(Version v1, Version v2) {
    if (v1 == null || v2 == null) { throw new NullPointerException(); }
    return ((v1.major() == v2.major()) && (v1.minor() == v2.minor()));
  }

}
