/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;


/**
 * Allows you to build valid config for the L2s. This class <strong>MUST NOT</strong> invoke the actual XML beans to do
 * its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class L2SConfigBuilder extends BaseConfigBuilder {

  private L2ConfigBuilder[] l2s;
  private HaConfigBuilder ha;
  
  public L2SConfigBuilder() {
    super(1, new String[] { "l2s", "ha" });
  }

  public void setL2s(L2ConfigBuilder[] l2s) {
    this.l2s = l2s;
    setProperty("l2s", l2s);
  }
  
  public void setHa(HaConfigBuilder ha) {
    this.ha = ha;
    setProperty("ha", ha);
  }

  public L2ConfigBuilder[] getL2s() {
    return l2s;
  }
  
  public HaConfigBuilder getHa() {
    return ha;
  }
  
  public String toString() {
    String out = "";
    if(isSet("l2s")) {
      out += l2sToString();
    }
    if(isSet("ha")) {
      out += ha.toString();
    }
    return out;
  }
  
  private String l2sToString() {
    String val = "";
    for (int i = 0; i < l2s.length; i++) {
      val += l2s[i].toString();
    }
    return val;
  }

  public static L2SConfigBuilder newMinimalInstance() {
    L2ConfigBuilder l2 = new L2ConfigBuilder();
//    l2.setName("localhost");
//    l2.setDSOPort(9510);

    HaConfigBuilder ha = new HaConfigBuilder();

    L2SConfigBuilder out = new L2SConfigBuilder();
    out.setL2s(new L2ConfigBuilder[] { l2 });
    out.setHa(ha);

    return out;
  }

}
