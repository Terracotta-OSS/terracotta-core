/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.test.schema;

public class PortConfigBuilder extends BaseConfigBuilder {
  public static enum PortType {
    JMXPORT, TSAPORT, GROUPPORT
  }

  private static final String   BIND           = "bind";
  private static final String   TSA_PORT       = "tsa-port";
  private static final String   JMX_PORT       = "jmx-port";
  private static final String   GROUP_PORT     = "tsa-group-port";
  private static final String[] ALL_PROPERTIES = concat(new Object[] { BIND, TSA_PORT, JMX_PORT, GROUP_PORT });

  private String                bindAddress;
  private int                   bindPort;
  private String                portType;

  public PortConfigBuilder(PortType portType) {
    super(7, ALL_PROPERTIES);
    setPort(portType);
  }

  PortConfigBuilder(int indent, PortType portType) {
    super(indent, ALL_PROPERTIES);
    setPort(portType);
  }

  private void setPort(PortType portType) {
    switch (portType) {
      case JMXPORT:
        this.portType = JMX_PORT;
        break;
      case TSAPORT:
        this.portType = TSA_PORT;
        break;
      case GROUPPORT:
        this.portType = GROUP_PORT;
        break;
      default:
        throw new RuntimeException("invalid port type " + portType);
    }
  }

  public void setBindAddress(String bindAddress) {
    this.bindAddress = bindAddress;
  }

  public void setBindPort(int bindPort) {
    setProperty(this.portType, bindPort);
    this.bindPort = bindPort;
  }

  @Override
  public String toString() {
    String out = "";
    out += indent() + "<" + this.portType;

    if (this.bindAddress != null) {
      out += " bind=" + "\"" + this.bindAddress + "\"";
    }

    out += ">" + this.bindPort + "</" + this.portType + ">";

    return out;
  }
}
