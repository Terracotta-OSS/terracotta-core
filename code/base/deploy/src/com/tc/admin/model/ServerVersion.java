/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

public class ServerVersion {

  private final String version;
  private final String buildID;
  private final String license;
  private final String copyright;

  public ServerVersion(String version, String buildID, String license, String copyright) {
    this.version = version;
    this.buildID = buildID;
    this.license = license;
    this.copyright = copyright;
  }

  public String version() {
    return this.version;
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
