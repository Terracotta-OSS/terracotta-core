/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.util.ProductInfo;

public class ServerVersion {

  private final String version;
  private final String patchVersion;
  private final String buildID;
  private final String license;
  private final String copyright;

  public ServerVersion() {
    this.version = ProductInfo.UNKNOWN_VALUE;
    this.patchVersion = ProductInfo.UNKNOWN_VALUE;
    this.buildID = ProductInfo.UNKNOWN_VALUE;
    this.license = ProductInfo.UNKNOWN_VALUE;
    this.copyright = ProductInfo.UNKNOWN_VALUE;
  }
  
  public ServerVersion(String version, String patchVersion, String buildID, String license, String copyright) {
    this.version = version;
    this.patchVersion = patchVersion;
    this.buildID = buildID;
    this.license = license;
    this.copyright = copyright;
  }

  public String version() {
    return this.version;
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
