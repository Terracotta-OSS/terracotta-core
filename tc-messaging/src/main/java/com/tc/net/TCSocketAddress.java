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
package com.tc.net;

import com.tc.exception.TCRuntimeException;
import com.tc.net.core.ConnectionInfo;
import com.tc.util.Assert;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * An emulation (incomplete) of the java.net.InetSocketAddress class (which happens to only be available in 1.4
 * runtimes) In the TC world, this class is used to describe a location (address + port) to connnect to, and/or listen
 * on
 * 
 * @author teck
 */
public class TCSocketAddress {
  /**
   * String form the loopback adaptor address (ie. 127.0.0.1)
   */
  public static final String      LOOPBACK_IP    = "127.0.0.1";

  /**
   * String form the loopback adaptor address (ie. 127.0.0.1)
   */
  public static final String      LOOPBACK_IPv6  = "::1";

  /**
   * String form of the wildcard IP address (ie. 0.0.0.0)
   */
  public static final String      WILDCARD_IP    = "0.0.0.0";

  /**
   * String form of the wildcard IPv6 address (ie. ::)
   */
  public static final String      WILDCARD_IPv6  = "::";

  /**
   * java.net.InetAddress form of the wildcard IPv4 address (ie. 0.0.0.0)
   */
  public static final InetAddress WILDCARD_ADDR;

  /**
   * java.net.InetAddress form of the wildcard IPv6 address (ie. ::)
   */
  public static final InetAddress WILDCARD_ADDR_IPv6;

  /**
   * java.net.InetAddress form of the wildcard IPv4 address (ie. 127.0.0.1)
   */
  public static final InetAddress LOOPBACK_ADDR;

  /**
   * java.net.InetAddress form of the wildcard IPv6 address (ie. ::1)
   */
  public static final InetAddress LOOPBACK_ADDR_IPv6;

  static {
    try {
      WILDCARD_ADDR = InetAddress.getByName(WILDCARD_IP);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + WILDCARD_IP);
    }

    try {
      WILDCARD_ADDR_IPv6 = InetAddress.getByName(WILDCARD_IPv6);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + WILDCARD_IPv6);
    }

    try {
      LOOPBACK_ADDR = InetAddress.getByName(LOOPBACK_IP);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + LOOPBACK_IP);
    }

    try {
      LOOPBACK_ADDR_IPv6 = InetAddress.getByName(LOOPBACK_IPv6);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + LOOPBACK_IPv6);
    }
  }

  private String                  stringForm;
  private String                  canonicalStringForm;

  // TODO: add a constructor that takes the output of toStringForm() and
  // reconstitutes a TCSocketAddress instance

  public TCSocketAddress(ConnectionInfo connInfo) throws UnknownHostException {
    this(connInfo.getHostname(), connInfo.getPort());
  }

  /**
   * Creates an address for localhost on the given port
   * 
   * @param port the port number, can be zero
   * @throws IllegalArgumentException if port is out of range (0 - 65535)
   */
  public TCSocketAddress(int port) {
    this(LOOPBACK_ADDR, port);
  }

  /**
   * Creates an address for localhost on the given port
   * 
   * @param port the port number, can be zero
   * @throws UnknownHostException
   * @throws IllegalArgumentException if port is out of range (0 - 65535)
   * @throws UnknownHostException if the host name provided can not be resolved
   */
  public TCSocketAddress(String host, int port) throws UnknownHostException {
    this(InetAddress.getByName(host), port);
  }

  /**
   * Create an TCSocketAdress instance for the gven address on the given port
   * 
   * @param addr the address to connect to. If null, this constructor behaves exactly like
   *        <code>TCSocketAddress(int port)</code>
   * @param port the port number, can be zero
   * @throws IllegalArgumentException if port is out of range (0 - 65535)
   */
  public TCSocketAddress(InetAddress addr, int port) {
    if (!isValidPort(port)) { throw new IllegalArgumentException("port (" + port + ") is out of range (0 - 0xFFFF)"); }

    if (addr == null) {
      try {
        addr = InetAddress.getLocalHost();
      } catch (UnknownHostException e) {
        addr = LOOPBACK_ADDR;
      }
    }

    this.addr = addr;
    this.port = port;

    Assert.eval(this.addr != null);
  }

  public InetAddress getAddress() {
    return addr;
  }

  public int getPort() {
    return port;
  }

  public byte[] getAddressBytes() {
    return addr.getAddress();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TCSocketAddress) {
      TCSocketAddress other = (TCSocketAddress) obj;
      return ((this.port == other.port) && this.addr.equals(other.addr));
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (addr == null) { return super.hashCode(); }

    return addr.hashCode() + port;
  }

  @Override
  public String toString() {
    return getStringForm();
  }

  /**
   * Returns a string description of this address instance in the following format: X.X.X.X:port where "X.X.X.X" is the
   * IP address and "port" is the port number The only purpose of this method is to document the specific format as a
   * contract. <code>toString()</code> is <b>not </b> required follow the same format
   * 
   * @return string form of this address
   */
  public String getStringForm() {
    if (stringForm == null) {
      StringBuffer buf = new StringBuffer();
      String hostAddr = addr.getHostAddress();
      boolean isPhysicalIPv6 = addr instanceof Inet6Address && hostAddr.contains(":");
      if (isPhysicalIPv6) {
        buf.append("[");
      }
      buf.append(hostAddr);
      if (isPhysicalIPv6) {
        buf.append("]");
      }
      buf.append(":").append(port);
      stringForm = buf.toString();
    }
    return stringForm;
  }

  /**
   * Return string form using canonical host name.
   */
  public String getCanonicalStringForm() {
    if (canonicalStringForm == null) {
      StringBuffer buf = new StringBuffer();
      String hostName = addr.getCanonicalHostName();
      boolean isPhysicalIPv6 = addr instanceof Inet6Address && hostName.contains(":");
      if (isPhysicalIPv6) {
        buf.append("[");
      }
      buf.append(hostName);
      if (isPhysicalIPv6) {
        buf.append("]");
      }
      buf.append(":").append(port);
      canonicalStringForm = buf.toString();
    }
    return canonicalStringForm;
  }

  public static boolean isValidPort(int port) {
    return ((port >= 0) && (port <= 0xFFFF));
  }

  private final InetAddress addr;
  private final int         port;
}
