/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

/**
 * Allows you to build valid config for the L2s. This class <strong>MUST NOT</strong> invoke the actual XML beans to do
 * its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class L2SConfigBuilder extends BaseConfigBuilder {

  public L2SConfigBuilder() {
    super(1, new String[] { "servers" });
  }

  public void setL2s(String l2s) {
    setProperty("servers", l2s);
  }

  public void setL2s(L2ConfigBuilder[] l2s) {
    setProperty("servers", selfTaggingArray(l2s));
  }
  
  public L2ConfigBuilder[] getL2s() {
    if (isSet("servers")) return (L2ConfigBuilder[]) ((SelfTaggingArray) getRawProperty("servers")).values();
    else return null;
  }

  public String toString() {
    if (!isSet("servers")) return "";
    else return getProperty("servers").toString();
  }

  public static L2SConfigBuilder newMinimalInstance() {
    L2ConfigBuilder l2 = new L2ConfigBuilder();
    l2.setName("localhost");
    l2.setDSOPort(9510);

    L2SConfigBuilder out = new L2SConfigBuilder();
    out.setL2s(new L2ConfigBuilder[] { l2 });

    return out;
  }

}
