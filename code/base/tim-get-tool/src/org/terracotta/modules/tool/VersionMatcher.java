/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool;

/**
 * This class encapsulates knowledge about whether a particular module or modules
 * are valid within the current tc/api version.  
 */

public class VersionMatcher {

  public static final String ANY_VERSION = "*";
  
  private final String tcVersion;
  private final String apiVersion;
  
  public VersionMatcher(String tcVersion, String apiVersion) {
    if(tcVersion == null || tcVersion.equals(ANY_VERSION)) {
      throw new IllegalArgumentException("Invalid tcVersion: " + tcVersion);
    }
    
    if(apiVersion == null || apiVersion.equals(ANY_VERSION)) {
      throw new IllegalArgumentException("Invalid apiVersion: " + apiVersion);
    }
    
    this.tcVersion = tcVersion;
    this.apiVersion = apiVersion;
  }
  
  /**
   * Determine whether a module's tc and api versions mean that it matches
   * with the current Terracotta installation's tc and api versions.  
   * 
   * @param moduleTcVersion is expected to be: * or exact like 3.0.0
   * @param moduleApiVersion is expected to be: * or exact like 1.0.0 or (most likely) a range [1.0.0,1.1.0)
   * @return true if module is suitable for this installation
   */
  public boolean matches(String moduleTcVersion, String moduleApiVersion) {
    return tcMatches(moduleTcVersion) && apiMatches(moduleApiVersion);
  }
  
  private boolean tcMatches(String moduleTcVersion) {
    return ANY_VERSION.equals(moduleTcVersion) || tcVersion.equals(moduleTcVersion);    
  }
  
  private boolean apiMatches(String moduleApiVersion) {
    if(ANY_VERSION.equals(moduleApiVersion)) {
      return true;
    } else {
      return new VersionRange(moduleApiVersion).contains(apiVersion);
    }
   }
}
