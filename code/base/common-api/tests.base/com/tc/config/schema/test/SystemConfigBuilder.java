/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

/**
 * Allows you to build valid config for the system-global config stuff. This class <strong>MUST NOT</strong> invoke the
 * actual XML beans to do its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class SystemConfigBuilder extends BaseConfigBuilder {

  public SystemConfigBuilder() {
    super(1, ALL_PROPERTIES);
  }

  public void setLicenseLocation(String value) {
    setProperty("location", value);
  }

  public static final String LICENSE_TYPE_NONE       = "none";
  public static final String LICENSE_TYPE_TRIAL      = "trial";
  public static final String LICENSE_TYPE_PRODUCTION = "production";

  public void setLicenseType(String value) {
    setProperty("type", value);
  }

  public static final String CONFIG_MODEL_DEVELOPMENT = "development";
  public static final String CONFIG_MODEL_PRODUCTION  = "production";

  public void setConfigurationModel(String value) {
    setProperty("configuration-model", value);
  }

  private static final String[] LICENSE              = new String[] { "location", "type" };
  private static final String[] TOP_LEVEL_PROPERTIES = new String[] { "configuration-model" };
  private static final String[] ALL_PROPERTIES       = concat(new Object[] { LICENSE, TOP_LEVEL_PROPERTIES });

  public String toString() {
    return elementGroup("license", LICENSE) + elements(TOP_LEVEL_PROPERTIES);
  }

  public static SystemConfigBuilder newMinimalInstance() {
    return new SystemConfigBuilder();
  }

}