/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.util.Assert;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Data used by the admin tool about each L2.
 */
public class L2Info implements java.io.Serializable {
  static final long          serialVersionUID = 7607194631717518924L;

  public static final String IMPLICIT_L2_NAME = "localhost";

  private final String       name;
  private final String       host;
  private InetAddress        hostAddress;
  private final int          jmxPort;
  private Integer            hashCode;

  public L2Info(String name, String host, int jmxPort) {
    Assert.assertNotBlank(host);
    Assert.eval(jmxPort >= 0);

    this.name = name;
    this.host = host;
    this.jmxPort = jmxPort;

    safeGetHostAddress();
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
    if (hostAddress != null) return hostAddress;
    if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
      hostAddress = InetAddress.getLocalHost();
    } else {
      hostAddress = InetAddress.getByName(host);
    }
    return hostAddress;
  }

  public String getCanonicalHostName() throws UnknownHostException {
    return getInetAddress().getCanonicalHostName();
  }

  public String safeGetCanonicalHostName() {
    try {
      return getCanonicalHostName();
    } catch (UnknownHostException uhe) {
      return null;
    }
  }

  public String getHostAddress() throws UnknownHostException {
    return getInetAddress().getHostAddress();
  }

  public String safeGetHostAddress() {
    try {
      return getInetAddress().getHostAddress();
    } catch (UnknownHostException uhe) {
      return null;
    }
  }

  public int jmxPort() {
    return this.jmxPort;
  }

  @Override
  public int hashCode() {
    if (hashCode == null) {
      HashCodeBuilder builder = new HashCodeBuilder();
      if (name != null) {
        builder.append(name);
      }
      builder.append(jmxPort);
      builder.append(host);
      hashCode = new Integer(builder.toHashCode());
    }
    return hashCode.intValue();
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof L2Info)) return false;
    L2Info other = (L2Info) object;
    return StringUtils.equals(name(), other.name()) && jmxPort() == other.jmxPort()
           && StringUtils.equals(host(), other.host());
  }

  public boolean matches(L2Info other) {
    if (!StringUtils.equals(name(), other.name())) return false;
    if (jmxPort() != other.jmxPort()) return false;
    String hostname = safeGetCanonicalHostName();
    String otherHostname = other.safeGetCanonicalHostName();
    if (hostname != null || otherHostname != null) {
      return StringUtils.equals(hostname, otherHostname);
    } else {
      return StringUtils.equals(host(), other.host());
    }
  }
}