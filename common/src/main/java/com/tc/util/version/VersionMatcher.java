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

  public VersionMatcher(String tcVersion) {
    if (tcVersion == null || tcVersion.equals(ANY_VERSION)) { throw new IllegalArgumentException("Invalid tcVersion: "
                                                                                                 + tcVersion); }

    this.tcVersion = tcVersion;
  }

  /**
   * Determine whether a module's tc version matches with the current Terracotta installation's tc
   * version.
   * 
   * @param moduleTcVersion is expected to be: * or exact like 3.0.0
   * @return true if module is suitable for this installation
   */
  public boolean matches(String moduleTcVersion) {
    return tcMatches(moduleTcVersion);
  }

  private boolean tcMatches(String moduleTcVersion) {
    return ANY_VERSION.equals(moduleTcVersion) /* || tcVersion.equals("[unknown]") */
           || tcVersion.equals(moduleTcVersion);
  }

}
