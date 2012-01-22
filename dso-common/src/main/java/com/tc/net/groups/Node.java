/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;


public class Node {

  private final String host;
  private final int    port;
  private final int    groupPort;
  private final int    hashCode;

  public Node(final String host, final int port) {
    this(host, port, 0);
  }

  public Node(final String host, final int port, final int groupPort) {
    checkArgs(host, port);
    this.host = host.trim();
    this.port = port;
    this.groupPort = groupPort;
    this.hashCode = (host + "-" + port).hashCode();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public int getGroupPort() {
    return groupPort;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Node) {
      Node that = (Node) obj;
      return this.port == that.port && this.host.equals(that.host);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  private static void checkArgs(final String host, final int port) throws IllegalArgumentException {
    if (host == null || host.trim().length() == 0) { throw new IllegalArgumentException("Invalid host name: " + host); }
    if (port < 0) { throw new IllegalArgumentException("Invalid port number: " + port); }
  }

  @Override
  public String toString() {
    return "Node{host=" + host + ":" + port + "}";
  }

  public String getServerNodeName() {
    return (host + ":" + port);
  }

}
