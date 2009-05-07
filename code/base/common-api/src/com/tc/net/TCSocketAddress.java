/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net;

import com.tc.exception.TCRuntimeException;
import com.tc.net.core.ConnectionInfo;
import com.tc.util.Assert;

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
   * Bytes for IPv4 wildcard address (ie. 0.0.0.0). This address is used to bind a listening socket to all available
   * interfaces
   */
  private static final byte[]     WILDCARD_BYTES = new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };

  /**
   * Bytes for the loopback adapator (ie. 127.0.0.1)
   */
  private static final byte[]     LOOPBACK_BYTES = new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 1 };

  /**
   * String form the loopback adaptor address (ie. 127.0.0.1)
   */
  public static final String      LOOPBACK_IP    = "127.0.0.1";

  /**
   * String form of the wildcard IP address (ie. 0.0.0.0)
   */
  public static final String      WILDCARD_IP    = "0.0.0.0";

  /**
   * java.net.InetAddress form of the wildcard IP address (ie. 0.0.0.0)
   */
  public static final InetAddress WILDCARD_ADDR;

  /**
   * java.net.InetAddress form of the wildcard IP address (ie. 127.0.0.1)
   */
  public static final InetAddress LOOPBACK_ADDR;

  static {
    try {
      WILDCARD_ADDR = InetAddress.getByName(WILDCARD_IP);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + WILDCARD_IP);
    }

    try {
      LOOPBACK_ADDR = InetAddress.getByName(LOOPBACK_IP);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException("Cannot create InetAddress instance for " + LOOPBACK_IP);
    }
  }

  private String                  stringForm;
  private String                  canonicalStringForm;

  public static byte[] getLoopbackBytes() {
    return LOOPBACK_BYTES.clone();
  }

  public static byte[] getWilcardBytes() {
    return WILDCARD_BYTES.clone();
  }

  // TODO: add a constructor that takes the output of toStringForm() and
  // reconsitutes a TCSocketAddress instance

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

  public boolean equals(Object obj) {
    if (obj instanceof TCSocketAddress) {
      TCSocketAddress other = (TCSocketAddress) obj;
      return ((this.port == other.port) && this.addr.equals(other.addr));
    }
    return false;
  }

  public int hashCode() {
    if (addr == null) { return super.hashCode(); }

    return addr.hashCode() + port;
  }

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
      buf.append(addr.getHostAddress()).append(":").append(port);
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
      buf.append(addr.getCanonicalHostName()).append(":").append(port);
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