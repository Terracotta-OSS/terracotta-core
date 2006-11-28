/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin;

public class ServerDescriptor {
  private String hostname;
  private int    portNumber;
  
  public ServerDescriptor(String hostname, int portNumber) {
    this.hostname   = hostname;
    this.portNumber = portNumber;
  }
  
  public String getHostname() {
    return this.hostname;
  }
  
  public int getPortNumber() {
    return this.portNumber;
  }
}
