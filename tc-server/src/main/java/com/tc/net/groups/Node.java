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
package com.tc.net.groups;


import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class Node {

  private static final String DELIMITER = ":";

  private final String host;
  private final int    port;
  private final int    groupPort;
  private final int    hashCode;

  public Node(String host, int port) {
    this(host, port, 0);
  }

  public Node(String host, int port, int groupPort) {
    checkArgs(host, port, groupPort);
    this.host = host.trim();
    this.port = port;
    this.groupPort = groupPort;
    this.hashCode = Objects.hash(host, port);
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

  private static void checkArgs(String host, int port, int groupPort) throws IllegalArgumentException {
    if (host == null || host.trim().length() == 0) { throw new IllegalArgumentException("Invalid host name: " + host); }
    if (port < 0) { throw new IllegalArgumentException("Invalid port number: " + port); }
    if (groupPort < 0) { throw new IllegalArgumentException("Invalid group port number: " + groupPort); }
  }

  @Override
  public String toString() {
    return "Node{host=" + getName() + "}";
  }

  public String getServerNodeName() {
    return getName();
  }

  private String getName() {
    return host + DELIMITER + port;
  }

  private static String getHost(String hostPort) {
    requireNonNull(hostPort);
    return hostPort.split(DELIMITER)[0];
  }

  private static int getPort(String hostPort) {
    requireNonNull(hostPort);
    String[] tokens = hostPort.split(DELIMITER);
    if (tokens.length != 2) {
      throw new IllegalArgumentException("Unexpected format: " + hostPort + " expected: host" + DELIMITER + "port");
    }
    return Integer.parseInt(tokens[1]);
  }
}
