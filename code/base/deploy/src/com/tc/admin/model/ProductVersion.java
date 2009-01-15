/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.util.ProductInfo;

public class ProductVersion implements IProductVersion {

  private final String version;
  private final String patchLevel;
  private final String patchVersion;
  private final String buildID;
  private final String license;
  private final String copyright;

  public ProductVersion() {
    this.version = ProductInfo.UNKNOWN_VALUE;
    this.patchLevel = ProductInfo.UNKNOWN_VALUE;
    this.patchVersion = ProductInfo.UNKNOWN_VALUE;
    this.buildID = ProductInfo.UNKNOWN_VALUE;
    this.license = ProductInfo.UNKNOWN_VALUE;
    this.copyright = ProductInfo.UNKNOWN_VALUE;
  }
  
  public ProductVersion(String version, String patchLevel, String patchVersion, String buildID, String license, String copyright) {
    this.version = version;
    this.patchLevel = patchLevel;
    this.patchVersion = patchVersion;
    this.buildID = buildID;
    this.license = license;
    this.copyright = copyright;
  }

  public String version() {
    return this.version;
  }

  public String patchLevel() {
    return this.patchLevel;
  }

  public String patchVersion() {
    return this.patchVersion;
  }
  
  public String license() {
    return this.license;
  }

  public String copyright() {
    return this.copyright;
  }

  public String buildID() {
    return this.buildID;
  }
}
