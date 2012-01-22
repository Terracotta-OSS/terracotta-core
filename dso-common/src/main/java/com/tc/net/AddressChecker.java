/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class AddressChecker {

  private final Set allLocalAddresses;

  public AddressChecker() {
    allLocalAddresses = findAllLocalAddresses();
  }

  public Set getAllLocalAddresses() {
    return allLocalAddresses;
  }

  public boolean isLegalBindAddress(InetAddress bindAddress) {
    if (bindAddress.isAnyLocalAddress()) { return true; }
    if (bindAddress.isLoopbackAddress()) { return true; }
    return allLocalAddresses.contains(bindAddress);
  }

  private Set findAllLocalAddresses() {
    Set rv = new HashSet();

    final Enumeration nics;
    try {
      nics = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      throw new RuntimeException(e);
    }

    while (nics.hasMoreElements()) {
      NetworkInterface nic = (NetworkInterface) nics.nextElement();
      Enumeration ips = nic.getInetAddresses();
      while (ips.hasMoreElements()) {
        rv.add(ips.nextElement());
      }
    }

    return Collections.unmodifiableSet(rv);
  }

}
