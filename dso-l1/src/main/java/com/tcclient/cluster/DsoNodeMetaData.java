/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tcclient.cluster;

public class DsoNodeMetaData {

  private final String ip;
  private final String hostname;

  public DsoNodeMetaData(final String ip, final String hostname) {
    this.ip = ip;
    this.hostname = hostname;
  }

  public String getIp() {
    return ip;
  }

  public String getHostname() {
    return hostname;
  }
}
