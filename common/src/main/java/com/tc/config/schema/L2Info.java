/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
  private final int          tsaPort;
  private final String       tsaGroupBind;
  private final int          tsaGroupPort;
  private final String       securityHostname;
  private Integer            hashCode;

  public L2Info(String name, String host, int jmxPort, int tsaPort,
                String tsaGroupBind, int tsaGroupPort, String securityHostname) {
    Assert.assertNotBlank(host);
    Assert.eval(jmxPort >= 0);

    this.name = name;
    this.host = host;
    this.jmxPort = jmxPort;
    this.tsaPort = tsaPort;
    this.tsaGroupBind = tsaGroupBind;
    this.tsaGroupPort = tsaGroupPort;
    this.securityHostname = securityHostname;

    safeGetHostAddress();
  }

  public L2Info(L2Info other) {
    this(other.name(), other.host(), other.jmxPort(), other.tsaPort(), other.tsaGroupBind(), other.tsaGroupPort(), other.securityHostname());
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

  public int tsaPort() {
    return this.tsaPort;
  }

  public String tsaGroupBind() {
    return tsaGroupBind;
  }

  public int tsaGroupPort() {
    return this.tsaGroupPort;
  }

  public String securityHostname() {
    return this.securityHostname;
  }

  @Override
  public int hashCode() {
    if (hashCode == null) {
      HashCodeBuilder builder = new HashCodeBuilder();
      if (name != null) {
        builder.append(name);
      }
      builder.append(jmxPort);
      builder.append(tsaPort);
      if (tsaGroupBind != null) {
        builder.append(tsaGroupBind);
      }
      builder.append(tsaGroupPort);
      builder.append(securityHostname);
      builder.append(host);
      hashCode = Integer.valueOf(builder.toHashCode());
    }
    return hashCode.intValue();
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof L2Info)) return false;
    L2Info other = (L2Info) object;
    return StringUtils.equals(name(), other.name())
           && jmxPort() == other.jmxPort()
           && tsaPort() == other.tsaPort()
           && tsaGroupPort() == other.tsaGroupPort()
           && StringUtils.equals(tsaGroupBind(), other.tsaGroupBind())
           && StringUtils.equals(securityHostname(), other.securityHostname())
           && StringUtils.equals(host(), other.host());
  }

  public boolean matches(L2Info other) {
    if (!StringUtils.equals(name(), other.name())) return false;
    if (jmxPort() != other.jmxPort()) return false;
    if (tsaPort() != other.tsaPort()) return false;
    if (tsaGroupPort() != other.tsaGroupPort()) return false;
    if (!StringUtils.equals(securityHostname(), other.securityHostname())) return false;
    if (!StringUtils.equals(tsaGroupBind(), other.tsaGroupBind())) return false;
    String hostname = safeGetCanonicalHostName();
    String otherHostname = other.safeGetCanonicalHostName();
    if (hostname != null || otherHostname != null) {
      return StringUtils.equals(hostname, otherHostname);
    } else {
      return StringUtils.equals(host(), other.host());
    }
  }
}
