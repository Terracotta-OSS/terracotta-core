/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.TCSocketAddress;

public class Node {

  private final String host;
  private final String nodeName;
  private final int    port;
  private final int    groupPort;
  private final int    hashCode;
  private final String bind;

  public Node(final String host, final int port) {
    this(host, port, TCSocketAddress.WILDCARD_IP);
  }
  
  public Node(final String host, final String nodeName, final int port) {
    this(host, port, 0, TCSocketAddress.WILDCARD_IP, nodeName);
  }

  public Node(final String host, final int port, final String bind) {
    this(host, port, 0, bind);
  }

  public Node(final String host, final int port, final int groupPort, final String bind) {
    this(host, port, groupPort, bind, host + ":" + port);
  }

  public Node(final String host, final int port, final int groupPort, final String bind, final String nodeName) {
    checkArgs(host, port);
    this.host = host.trim();
    this.port = port;
    this.groupPort = groupPort;
    this.bind = bind; // not part of equals()
    this.nodeName = nodeName;
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

  public String getBind() {
    return bind;
  }
  
  public String getNodeName() {
    return nodeName;
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
    return "Node{host=" + host + ":" + port + " server name=" + nodeName + "}";
  }

  public String getServerNodeName() {
    return (host + ":" + port);
  }

}
