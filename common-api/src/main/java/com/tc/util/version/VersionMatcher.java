/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.version;

/**
 * This class encapsulates knowledge about whether a particular module or modules are valid within the current tc/api
 * version.
 */

public class VersionMatcher {

  public static final String ANY_VERSION = "*";

  private final String       tcVersion;
  private final String       timApiVersion;

  public VersionMatcher(String tcVersion, String timApiVersion) {
    if (tcVersion == null || tcVersion.equals(ANY_VERSION)) { throw new IllegalArgumentException("Invalid tcVersion: "
                                                                                                 + tcVersion); }

    if (timApiVersion == null || timApiVersion.equals(ANY_VERSION)) { throw new IllegalArgumentException(
                                                                                                         "Invalid apiVersion: "
                                                                                                             + timApiVersion); }

    this.tcVersion = tcVersion;
    this.timApiVersion = timApiVersion;
  }

  /**
   * Determine whether a module's tc and api versions mean that it matches with the current Terracotta installation's tc
   * and api versions.
   * 
   * @param moduleTcVersion is expected to be: * or exact like 3.0.0
   * @param moduleTimApiVersion is expected to be: * or exact like 1.0.0 or (most likely) a range [1.0.0,1.1.0)
   * @return true if module is suitable for this installation
   */
  public boolean matches(String moduleTcVersion, String moduleTimApiVersion) {
    return tcMatches(moduleTcVersion) && apiMatches(moduleTimApiVersion);
  }

  private boolean tcMatches(String moduleTcVersion) {
    return ANY_VERSION.equals(moduleTcVersion) /* || tcVersion.equals("[unknown]") */
           || tcVersion.equals(moduleTcVersion);
  }

  private boolean apiMatches(String moduleApiVersion) {
    if (ANY_VERSION.equals(moduleApiVersion)) {
      return true;
    } else {
      return new VersionRange(moduleApiVersion).contains(timApiVersion);
    }
  }
}
