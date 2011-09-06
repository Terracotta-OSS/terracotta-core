/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

/**
 * Defines some common attributes extracted from downloaded artifacts.  In general this information
 * is retrieved from the index and parsed into a Module's information but in a few cases
 * we check inside the artifact itself for verification purposes.
 */
public enum ManifestAttributes {

  OSGI_SYMBOLIC_NAME("Bundle-SymbolicName"),
  OSGI_VERSION("Bundle-Version"),
  OSGI_CATEGORY("Bundle-Category"),
  TERRACOTTA_COORDINATES("Terracotta-ArtifactCoordinates"),
  TERRACOTTA_CATEGOORY("Terracotta-Category");
  
  private String attribute;
  
  ManifestAttributes(String attribute) {
    this.attribute = attribute;
  }
  
  /**
   * Return the actual attribute value used in the manifest
   */
  public String attribute() {
    return this.attribute;
  }
  
  
}
