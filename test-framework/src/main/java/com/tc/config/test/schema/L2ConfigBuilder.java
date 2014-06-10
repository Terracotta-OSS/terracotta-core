/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.test.schema;

import com.tc.config.test.schema.PortConfigBuilder.PortType;

/**
 * Allows you to build valid config for an L2. This class <strong>MUST NOT</strong> invoke the actual XML beans to do
 * its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class L2ConfigBuilder extends BaseConfigBuilder {

  private String            host;
  private String            name;
  private PortConfigBuilder jmxPortBuilder   = null;
  private PortConfigBuilder tsaPortBuilder   = null;
  private PortConfigBuilder groupPortBuilder = null;
  private PortConfigBuilder managementPortBuilder = null;
  private boolean           offheapEnabled   = false;
  private String            offheapMaxDataSize;
  private boolean           security_enabled = false;
  private String            security_certificateUri;
  private String            security_keychainUrl;
  private String            security_keychainImpl;
  private String            security_secretProviderImpl;
  private String            security_authUrl;
  private String            security_authImpl;

  public L2ConfigBuilder() {
    super(3, ALL_PROPERTIES);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setHost(String host) {
    this.host = host;
  }

  String getName() {
    return this.name;
  }

  public void setData(String data) {
    setProperty("data", data);
  }

  public void setServerDbBackup(String serverDbBackup) {
    setProperty("data-backup", serverDbBackup);
  }

  public void setLogs(String logs) {
    setProperty("logs", logs);
  }

  public synchronized void setTSAPort(int data) {
    if (this.tsaPortBuilder == null) {
      this.tsaPortBuilder = new PortConfigBuilder(PortType.TSAPORT);
    }
    this.tsaPortBuilder.setBindPort(data);
  }

  public synchronized void setTSABindAddress(String data) {
    if (this.tsaPortBuilder == null) {
      this.tsaPortBuilder = new PortConfigBuilder(PortType.TSAPORT);
    }
    this.tsaPortBuilder.setBindAddress(data);
  }

  public synchronized void setJMXPort(int data) {
    if (this.jmxPortBuilder == null) {
      this.jmxPortBuilder = new PortConfigBuilder(PortType.JMXPORT);
    }
    this.jmxPortBuilder.setBindPort(data);
  }

  public synchronized void setManagementPort(int data) {
    if (this.managementPortBuilder == null) {
      this.managementPortBuilder = new PortConfigBuilder(PortType.MANAGEMENTPORT);
    }
    this.managementPortBuilder.setBindPort(data);
  }

  public synchronized void setManagementPortBindAddress(String data) {
    if (this.managementPortBuilder == null) {
      this.managementPortBuilder = new PortConfigBuilder(PortType.MANAGEMENTPORT);
    }
    this.managementPortBuilder.setBindAddress(data);
  }

  public synchronized void setJMXBindAddress(String data) {
    if (this.jmxPortBuilder == null) {
      this.jmxPortBuilder = new PortConfigBuilder(PortType.JMXPORT);
    }
    this.jmxPortBuilder.setBindAddress(data);
  }

  public synchronized void setTSAGroupPort(int data) {
    if (this.groupPortBuilder == null) {
      this.groupPortBuilder = new PortConfigBuilder(PortType.GROUPPORT);
    }
    this.groupPortBuilder.setBindPort(data);
  }

  public synchronized void setTSAGroupPortBindAddress(String data) {
    if (this.groupPortBuilder == null) {
      this.groupPortBuilder = new PortConfigBuilder(PortType.GROUPPORT);
    }
    this.groupPortBuilder.setBindAddress(data);
  }

  public void setPasswordFile(String data) {
    setProperty("password-file", data);
  }

  public void setAccessFile(String data) {
    setProperty("access-file", data);
  }

  public void setOffHeapEnabled(final boolean enabled) {
    this.offheapEnabled = enabled;
  }

  public void setOffHeapMaxDataSize(final String maxDataSize) {
    this.offheapMaxDataSize = maxDataSize;
  }

  public boolean isSecurityEnabled() {
    return security_enabled;
  }

  public void setSecurityEnabled(boolean enabled) {
    this.security_enabled = enabled;
  }

  public void setSecurityCertificateUri(String certificateUri) {
    this.security_certificateUri = certificateUri;
  }

  public void setSecurityKeychainUrl(String keychainUrl) {
    this.security_keychainUrl = keychainUrl;
  }

  public void setSecurityKeychainImpl(String securityKeychainImpl) {
    security_keychainImpl = securityKeychainImpl;
  }

  public void setSecuritySecretProviderImpl(String secretProviderImpl) {
    security_secretProviderImpl = secretProviderImpl;
  }

  public void setSecurityAuthUrl(String authUrl) {
    this.security_authUrl = authUrl;
  }

  public void setSecurityAuthImpl(String authImpl) {
    security_authImpl = authImpl;
  }

  private static final String[] L2             = new String[] { "data", "logs", "data-backup" };

  private static final String[] AUTHENTICATION = new String[] { "password-file", "access-file" };

  private static final String[] ALL_PROPERTIES = concat(new Object[] { L2, AUTHENTICATION });
  private static final boolean  JMX_ENABLED    = true;

  @Override
  public String toString() {
    String out = "";

    out += indent() + "<server host=" + (this.host != null ? "\"" + this.host + "\"" : "\"%i\"")
           + (this.name != null ? " name=\"" + this.name + "\"" : "") + " jmx-enabled=\"" + JMX_ENABLED + "\">\n";

    out += elements(L2) + getPortsConfig() + elementGroup("authentication", AUTHENTICATION) + getOffHeapConfig()
           + getSecurityConfig();

    out += closeElement("server");

    return out;
  }

  private String getOffHeapConfig() {
    if (!offheapEnabled) return "\n";
    String out = "\n";
    out += "<offheap>\n";
    out += "<enabled>" + offheapEnabled + "</enabled>\n";
    out += "<maxDataSize>" + offheapMaxDataSize + "</maxDataSize>\n";
    out += "</offheap>\n";
    return out;
  }

  private String getSecurityConfig() {
    if (!security_enabled) return "\n";
    String out = "\n";
    out += "<security>\n";
    out += "\t<ssl>\n";
    out += "\t\t<certificate>" + security_certificateUri + "</certificate>\n";
    out += "\t</ssl>\n";
    out += "\t<keychain>\n";
    out += "\t\t<class>" + security_keychainImpl + "</class>\n";
    out += "\t\t<url>" + security_keychainUrl + "</url>\n";
    out += "\t\t<secret-provider>" + security_secretProviderImpl + "</secret-provider>\n";
    out += "\t</keychain>\n";
    out += "\t<auth>\n";
    out += "\t\t<realm>" + security_authImpl + "</realm>\n";
    out += "\t\t<url>" + security_authUrl + "</url>\n";
    out += "\t</auth>\n";
    out += "</security>\n";
    return out;
  }

  private String getPortsConfig() {
    String out = "";

    if (this.tsaPortBuilder != null) {
      out += this.tsaPortBuilder.toString() + "\n";
    }
    if (this.jmxPortBuilder != null) {
      out += this.jmxPortBuilder.toString() + "\n";
    }
    if (this.groupPortBuilder != null) {
      out += this.groupPortBuilder.toString() + "\n";
    }
    if (this.managementPortBuilder != null) {
      out += this.managementPortBuilder.toString() + "\n";
    }

    return out;
  }

  public static L2ConfigBuilder newMinimalInstance() {
    return new L2ConfigBuilder();
  }

}
