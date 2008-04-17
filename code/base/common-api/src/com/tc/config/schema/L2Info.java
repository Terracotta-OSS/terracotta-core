/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.util.Assert;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Data used by the admin tool about each L2.
 */
public class L2Info implements java.io.Serializable {
  static final long serialVersionUID = 7607194631717518924L;
  
  public static final String IMPLICIT_L2_NAME = "localhost";

  private final String       name;
  private final String       host;
  private InetAddress        hostAddress;
  private final int          jmxPort;

  public L2Info(String name, String host, int jmxPort) {
    Assert.assertNotBlank(name);
    Assert.assertNotBlank(host);
    Assert.eval(jmxPort >= 0);

    this.name = name;
    this.host = host;
    this.jmxPort = jmxPort;
  }

  public L2Info(L2Info other) {
    this(other.name(), other.host(), other.jmxPort());
  }
  
  public String name() {
    return this.name;
  }

  public String host() {
    return this.host;
  }

  public InetAddress getInetAddress() throws UnknownHostException {
    if(hostAddress != null) return hostAddress;
    if("localhost".equals(host) || "127.0.0.1".equals(host)) {
      hostAddress = InetAddress.getLocalHost();
    } else {
      hostAddress = InetAddress.getByName(host);
    }
    return hostAddress;
  }
  
  public String getCanonicalHostName() throws UnknownHostException {
    return getInetAddress().getCanonicalHostName();
  }
  
  public String getHostAddress() throws UnknownHostException {
    return getInetAddress().getHostAddress();
  }
  
  public int jmxPort() {
    return this.jmxPort;
  }

}