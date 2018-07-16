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
package com.tc.net.core;

import com.tc.util.Assert;
import java.net.InetSocketAddress;

public class ConnectionInfo implements java.io.Serializable {

  public static final ConnectionInfo[] EMPTY_ARRAY = new ConnectionInfo[0];
  private final InetSocketAddress                 address;
  private final int                    server;

  public ConnectionInfo(String hostname, int port) {
    this(hostname, port, 0);
  }

  public ConnectionInfo(String hostname, int port, int server) {
    Assert.assertNotNull(hostname);
    Assert.assertTrue(port >= 0);
    this.address = InetSocketAddress.createUnresolved(hostname, port);
    this.server = server;
  }
  
  public InetSocketAddress getAddress() {
    return this.address;
  }

  public String getHostname() {
    return this.address.getHostString();
  }

  public int getPort() {
    return this.address.getPort();
  }
  
  public int getServer() {
    return server;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof ConnectionInfo) {
      ConnectionInfo other = (ConnectionInfo) o;
      return this.address.equals(other.getAddress());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return this.address.toString();
  }

}
