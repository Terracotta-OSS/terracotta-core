/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config.schema;

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
  private final int          managementPort;
  private final String       securityHostname;
  private Integer            hashCode;

  public L2Info(String name, String host, int jmxPort, int tsaPort,
                String tsaGroupBind, int tsaGroupPort, int managementPort, String securityHostname) {
    Assert.assertNotBlank(host);
    Assert.eval(jmxPort >= 0);

    this.name = name;
    this.host = host;
    this.jmxPort = jmxPort;
    this.tsaPort = tsaPort;
    this.tsaGroupBind = tsaGroupBind;
    this.tsaGroupPort = tsaGroupPort;
    this.managementPort = managementPort;
    this.securityHostname = securityHostname;

    safeGetHostAddress();
  }

  public L2Info(L2Info other) {
    this(other.name(), other.host(), other.jmxPort(), other.tsaPort(), other.tsaGroupBind(), other.tsaGroupPort(),
         other.managementPort(), other.securityHostname());
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

  public int managementPort() {
    return this.managementPort;
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
      final int prime = 31;
      int result = 1;
      result = prime * result + ((host == null) ? 0 : host.hashCode());
      result = prime * result + jmxPort;
      result = prime * result + managementPort;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((securityHostname == null) ? 0 : securityHostname.hashCode());
      result = prime * result + ((tsaGroupBind == null) ? 0 : tsaGroupBind.hashCode());
      result = prime * result + tsaGroupPort;
      result = prime * result + tsaPort;
      hashCode = result;
    }
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    L2Info other = (L2Info) obj;
    if (host == null) {
      if (other.host != null) return false;
    } else if (!host.equals(other.host)) return false;
    if (jmxPort != other.jmxPort) return false;
    if (managementPort != other.managementPort) return false;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    if (securityHostname == null) {
      if (other.securityHostname != null) return false;
    } else if (!securityHostname.equals(other.securityHostname)) return false;
    if (tsaGroupBind == null) {
      if (other.tsaGroupBind != null) return false;
    } else if (!tsaGroupBind.equals(other.tsaGroupBind)) return false;
    if (tsaGroupPort != other.tsaGroupPort) return false;
    if (tsaPort != other.tsaPort) return false;
    return true;
  }

}
