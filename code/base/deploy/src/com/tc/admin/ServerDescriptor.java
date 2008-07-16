/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

public class ServerDescriptor {
  private String hostname;
  private int    portNumber;

  public ServerDescriptor(String hostname, int portNumber) {
    this.hostname = hostname;
    this.portNumber = portNumber;
  }

  public String getHostname() {
    return this.hostname;
  }

  public int getPortNumber() {
    return this.portNumber;
  }
}
