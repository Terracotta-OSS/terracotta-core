/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.util.Assert;

/**
 * Represents the configuration model.
 */
public class ConfigurationModel {

  public static final ConfigurationModel DEVELOPMENT = new ConfigurationModel("development");
  public static final ConfigurationModel PRODUCTION  = new ConfigurationModel("production");

  private final String                   type;

  private ConfigurationModel(String type) {
    Assert.assertNotBlank(type);
    this.type = type;
  }

  public boolean equals(Object that) {
    return (that instanceof ConfigurationModel) && ((ConfigurationModel) that).type.equals(this.type);
  }

  public int hashCode() {
    return this.type.hashCode();
  }

  public String toString() {
    return this.type;
  }

}
