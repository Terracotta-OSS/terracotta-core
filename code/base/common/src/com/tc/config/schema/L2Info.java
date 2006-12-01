/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.util.Assert;

/**
 * Data used by the admin tool about each L2.
 */
public class L2Info implements java.io.Serializable {

  public static final String IMPLICIT_L2_NAME = "(implicit)";

  private final String       name;
  private final String       host;
  private final int          jmxPort;

  public L2Info(String name, String host, int jmxPort) {
    Assert.assertNotBlank(name);
    Assert.assertNotBlank(host);
    Assert.eval(jmxPort >= 0);

    this.name = name;
    this.host = host;
    this.jmxPort = jmxPort;
  }

  public String name() {
    return this.name;
  }

  public String host() {
    return this.host;
  }

  public int jmxPort() {
    return this.jmxPort;
  }

}