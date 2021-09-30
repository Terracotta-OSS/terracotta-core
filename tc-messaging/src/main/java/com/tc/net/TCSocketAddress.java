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

import java.net.InetAddress;
import java.net.InetSocketAddress;
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
  public static final String      COMPRESSED_LOOPBACK_IPv6  = "::1";


  public static final String      NATURAL_LOOPBACK_IPv6  = "0:0:0:0:0:0:0:1";

  /**
   * String form of the wildcard IP address (ie. 0.0.0.0)
   */
  public static final String      WILDCARD_IP    = "0.0.0.0";

  /**
   * String form of the wildcard IPv6 address (ie. ::)
   */
  public static final String      COMPRESSED_WILDCARD_IPv6  = "::";


  public static final String      NATURAL_WILDCARD_IPv6  = "0:0:0:0:0:0:0:0";
  /**
   * java.net.InetAddress form of the wildcard IPv4 address (ie. 0.0.0.0)
   */
  private static final InetAddress WILDCARD_ADDR;

  /**
   * java.net.InetAddress form of the wildcard IPv6 address (ie. ::)
   */
  private static final InetAddress WILDCARD_ADDR_IPv6;

  /**
   * java.net.InetAddress form of the wildcard IPv4 address (ie. 127.0.0.1)
   */
  private static final InetAddress LOOPBACK_ADDR;

  /**
   * java.net.InetAddress form of the wildcard IPv6 address (ie. ::1)
   */
  private static final InetAddress LOOPBACK_ADDR_IPv6;

  static {
    InetAddress lookup = null;
    try {
      WILDCARD_ADDR = InetAddress.getByName(WILDCARD_IP);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + WILDCARD_IP);
    }

    try {
      lookup = InetAddress.getByName(COMPRESSED_WILDCARD_IPv6);
    } catch (UnknownHostException e) {
      try {
        lookup = InetAddress.getByName(NATURAL_WILDCARD_IPv6);
      } catch (UnknownHostException f) {
        throw new TCRuntimeException("Cannot create InetAddress instance for " + NATURAL_WILDCARD_IPv6);
      }
    }
    WILDCARD_ADDR_IPv6 = lookup;

    try {
      LOOPBACK_ADDR = InetAddress.getByName(LOOPBACK_IP);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + LOOPBACK_IP);
    }

    lookup = null;
    try {
      lookup = InetAddress.getByName(COMPRESSED_LOOPBACK_IPv6);
    } catch (UnknownHostException e) {
      try {
        lookup = InetAddress.getByName(NATURAL_LOOPBACK_IPv6);
      } catch (UnknownHostException f) {
        throw new TCRuntimeException("Cannot create InetAddress instance for " + NATURAL_LOOPBACK_IPv6);
      }
    }
    LOOPBACK_ADDR_IPv6 = lookup;
  }

  /**
   * Returns a string description of this address instance in the following format: X.X.X.X:port where "X.X.X.X" is the
   * IP address and "port" is the port number The only purpose of this method is to document the specific format as a
   * contract. <code>toString()</code> is <b>not </b> required follow the same format
   * 
   * @return string form of this address
   */
  public static String getStringForm(InetSocketAddress addr) {
    StringBuilder buf = new StringBuilder();
    String hostAddr = addr.getHostString();
    if (!isWildcardAddress(hostAddr)) {
      boolean isPhysicalIPv6 = hostAddr.contains(":");
      if (isPhysicalIPv6) {
        buf.append("[");
      }
      buf.append(hostAddr);
      if (isPhysicalIPv6) {
        buf.append("]");
      }
    } else {
      buf.append("*");
    }
    buf.append(":").append(addr.getPort());
    return buf.toString();
  }

  /**
   * Return string form using canonical host name.
   */
  public static String getCanonicalStringForm(InetSocketAddress addr) {
    StringBuilder buf = new StringBuilder();
    String hostAddr = addr.getAddress().getCanonicalHostName();
    if (!isWildcardAddress(hostAddr)) {
      boolean isPhysicalIPv6 = hostAddr.contains(":");
      if (isPhysicalIPv6) {
        buf.append("[");
      }
      buf.append(hostAddr);
      if (isPhysicalIPv6) {
        buf.append("]");
      }
    }
    buf.append(":").append(addr.getPort());
    return buf.toString();
  }

  public static boolean isValidPort(int port) {
    return ((port >= 0) && (port <= 0xFFFF));
  }

  public static boolean isWildcardAddress(String txt) {
    return WILDCARD_IP.equals(txt) || COMPRESSED_WILDCARD_IPv6.equals(txt) || NATURAL_WILDCARD_IPv6.equals(txt);
  }

  public static boolean isLoopbackAddress(String txt) {
    return LOOPBACK_IP.equals(txt) || COMPRESSED_LOOPBACK_IPv6.equals(txt) || NATURAL_WILDCARD_IPv6.equals(txt);
  }
}
