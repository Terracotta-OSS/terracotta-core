/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.test.PortConfigBuilder.PortType;

/**
 * Allows you to build valid config for an L2. This class <strong>MUST NOT</strong> invoke the actual XML beans to do
 * its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class L2ConfigBuilder extends BaseConfigBuilder {

  private String            host;
  private String            name;
  private PortConfigBuilder jmxPortBuilder   = null;
  private PortConfigBuilder dsoPortBuilder   = null;
  private PortConfigBuilder groupPortBuilder = null;
  private boolean           offheap_enabled  = false;
  private String            offheap_maxDataSize;

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

  public void setStatistics(String statistics) {
    setProperty("statistics", statistics);
  }

  public void setServerDbBackup(String serverDbBackup) {
    setProperty("data-backup", serverDbBackup);
  }

  public void setLogs(String logs) {
    setProperty("logs", logs);
  }

  public synchronized void setDSOPort(int data) {
    if (this.dsoPortBuilder == null) {
      this.dsoPortBuilder = new PortConfigBuilder(PortType.DSOPORT);
    }
    this.dsoPortBuilder.setBindPort(data);
  }

  public synchronized void setDSOBindAddress(String data) {
    if (this.dsoPortBuilder == null) {
      this.dsoPortBuilder = new PortConfigBuilder(PortType.DSOPORT);
    }
    this.dsoPortBuilder.setBindAddress(data);
  }

  public synchronized void setJMXPort(int data) {
    if (this.jmxPortBuilder == null) {
      this.jmxPortBuilder = new PortConfigBuilder(PortType.JMXPORT);
    }
    this.jmxPortBuilder.setBindPort(data);
  }

  public synchronized void setJMXBindAddress(String data) {
    if (this.jmxPortBuilder == null) {
      this.jmxPortBuilder = new PortConfigBuilder(PortType.JMXPORT);
    }
    this.jmxPortBuilder.setBindAddress(data);
  }

  public synchronized void setL2GroupPort(int data) {
    if (this.groupPortBuilder == null) {
      this.groupPortBuilder = new PortConfigBuilder(PortType.GROUPPORT);
    }
    this.groupPortBuilder.setBindPort(data);
  }

  public synchronized void setL2GroupPortBindAddress(String data) {
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

  public static final String PERSISTENCE_MODE_TEMPORARY_SWAP_ONLY = "temporary-swap-only";
  public static final String PERSISTENCE_MODE_PERMANENT_STORE     = "permanent-store";

  public void setPersistenceMode(String data) {
    setProperty("mode", data);
  }

  public void setGCEnabled(boolean data) {
    setProperty("enabled", data);
  }

  public void setGCEnabled(String data) {
    setProperty("enabled", data);
  }

  public void setGCVerbose(boolean data) {
    setProperty("verbose", data);
  }

  public void setGCVerbose(String data) {
    setProperty("verbose", data);
  }

  public void setGCInterval(int data) {
    setProperty("interval", data);
  }

  public void setReconnectWindowForPrevConnectedClients(int secs) {
    setProperty("client-reconnect-window", secs);
  }

  public void setGCInterval(String data) {
    setProperty("interval", data);
  }

  public void setOffHeapEnabled(final boolean enabled) {
    this.offheap_enabled = enabled;
  }

  public void setOffHeapMaxDataSize(final String maxDataSize) {
    this.offheap_maxDataSize = maxDataSize;
  }

  private static final String[] L2                   = new String[] { "data", "logs", "data-backup", "statistics" };

  private static final String[] DSO_PERSISTENCE_MODE = new String[] { "mode" };
  private static final String[] DSO_PERSISTENCE      = concat(new Object[] { DSO_PERSISTENCE_MODE });

  private static final String[] DSO_RECONNECTWINDOW  = new String[] { "client-reconnect-window" };
  private static final String[] DSO_GC               = new String[] { "enabled", "verbose", "interval" };
  private static final String[] AUTHENTICATION       = new String[] { "password-file", "access-file" };

  private static final String[] DSO                  = concat(new Object[] { DSO_RECONNECTWINDOW, DSO_PERSISTENCE,
      DSO_GC                                        });
  private static final String[] ALL_PROPERTIES       = concat(new Object[] { L2, AUTHENTICATION, DSO });

  @Override
  public String toString() {
    String out = "";

    out += indent() + "<server host=" + (this.host != null ? this.host : "\"%i\"")
           + (this.name != null ? " name=\"" + this.name + "\"" : "") + ">\n";

    out += elements(L2) + getPortsConfig() + elementGroup("authentication", AUTHENTICATION) + openElement("dso", DSO)
           + elements(DSO_RECONNECTWINDOW) + openElement("persistence", DSO_PERSISTENCE)
           + elements(DSO_PERSISTENCE_MODE) + getOffHeapConfig() + closeElement("persistence", DSO_PERSISTENCE)
           + elementGroup("garbage-collection", DSO_GC) + closeElement("dso", DSO);

    out += closeElement("server");

    return out;
  }

  private String getOffHeapConfig() {
    if (!offheap_enabled) return "\n";
    String out = "\n";
    out += "<offheap>\n";
    out += "<enabled>" + offheap_enabled + "</enabled>\n";
    out += "<maxDataSize>" + offheap_maxDataSize + "</maxDataSize>\n";
    out += "</offheap>\n";
    return out;
  }

  private String getPortsConfig() {
    String out = "";

    if (this.dsoPortBuilder != null) {
      out += this.dsoPortBuilder.toString() + "\n";
    }
    if (this.jmxPortBuilder != null) {
      out += this.jmxPortBuilder.toString() + "\n";
    }
    if (this.groupPortBuilder != null) {
      out += this.groupPortBuilder.toString() + "\n";
    }

    return out;
  }

  public static L2ConfigBuilder newMinimalInstance() {
    return new L2ConfigBuilder();
  }

}
