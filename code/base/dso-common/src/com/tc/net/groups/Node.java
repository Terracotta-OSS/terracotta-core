/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

public class Node {

  private final String host;
  private final int    port;
  private final int    hashCode;

  public Node(final String host, final int port) {
    checkArgs(host, port);
    this.host = host.trim();
    this.port = port;
    this.hashCode = (host + "-" + port).hashCode();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public boolean equals(Object obj) {
    if (obj instanceof Node) {
      Node that = (Node) obj;
      return this.port == that.port && this.host.equals(that.host);
    }
    return false;
  }

  public int hashCode() {
    return hashCode;
  }

  private static void checkArgs(final String host, final int port) throws IllegalArgumentException {
    if (host == null || host.trim().length() == 0) { throw new IllegalArgumentException("Invalid host name: " + host); }
    if (port < 0) { throw new IllegalArgumentException("Invalid port number: " + port); }
  }

  public String toString() {
    return "Node{host=" + host + ":" + port + "}";
  }
}
