/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.test.schema;

/**
 * Allows you to build valid config for the entire system. This class <strong>MUST NOT</strong> invoke the actual XML
 * beans to do its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class TerracottaConfigBuilder extends BaseConfigBuilder {

  public TerracottaConfigBuilder() {
    super(0, ALL_PROPERTIES);
  }

  public void setSystem(String value) {
    setProperty("system", value);
  }

  public void setSystem(SystemConfigBuilder value) {
    setProperty("system", value);
  }

  public SystemConfigBuilder getSystem() {
    if (!isSet("system")) setSystem(SystemConfigBuilder.newMinimalInstance());
    return (SystemConfigBuilder) getRawProperty("system");
  }

  public void setClient(String value) {
    setProperty("clients", value);
  }

  public void setClient(L1ConfigBuilder value) {
    setProperty("clients", value);
  }

  public L1ConfigBuilder getClient() {
    if (!isSet("clients")) setClient(L1ConfigBuilder.newMinimalInstance());
    return (L1ConfigBuilder) getRawProperty("clients");
  }

  // public void setServers(String value) {
  // setProperty("servers", value);
  // }

  public void setServers(L2SConfigBuilder value) {
    setProperty("servers", value);
  }

  public L2SConfigBuilder getServers() {
    if (!isSet("servers")) setServers(L2SConfigBuilder.newMinimalInstance());
    return (L2SConfigBuilder) getRawProperty("servers");
  }

  public void setApplication(String value) {
    setProperty("application", value);
  }

  public void setApplication(ApplicationConfigBuilder value) {
    setProperty("application", value);
  }

  public void setTcProperties(TcPropertiesBuilder value) {
    setProperty("tc-properties", value);
  }

  public ApplicationConfigBuilder getApplication() {
    if (!isSet("application")) setApplication(ApplicationConfigBuilder.newMinimalInstance());
    return (ApplicationConfigBuilder) getRawProperty("application");
  }

  public static final String[] ALL_PROPERTIES = new String[] { "system", "clients", "servers", "application",
      "tc-properties"                        };

  @Override
  public String toString() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n\n"
           + "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">\n" + elements(ALL_PROPERTIES)
           + "\n</tc:tc-config>";
  }

  public static TerracottaConfigBuilder newMinimalInstance() {
    SystemConfigBuilder system = SystemConfigBuilder.newMinimalInstance();
    L2SConfigBuilder l2s = L2SConfigBuilder.newMinimalInstance();
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();
    out.setSystem(system);
    out.setServers(l2s);
    return out;
  }

  public static void main(String[] args) {
    System.err.println(newMinimalInstance());
  }

}
