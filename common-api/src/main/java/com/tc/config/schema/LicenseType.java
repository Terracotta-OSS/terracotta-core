/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.util.Assert;

/**
 * Represents the type of the license.
 */
public class LicenseType {

  public static final LicenseType NONE       = new LicenseType("none");
  public static final LicenseType TRIAL      = new LicenseType("trial");
  public static final LicenseType PRODUCTION = new LicenseType("production");

  private final String            type;

  private LicenseType(String type) {
    Assert.assertNotBlank(type);
    this.type = type;
  }

  public boolean equals(Object that) {
    return (that instanceof LicenseType) && ((LicenseType) that).type.equals(this.type);
  }

  public int hashCode() {
    return this.type.hashCode();
  }

  public String toString() {
    return this.type;
  }
  
  public String getType() {
    return this.type;
  }

}
