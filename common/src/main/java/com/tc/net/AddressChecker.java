/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net;

import com.tc.util.concurrent.ThreadUtil;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressChecker {

  private static Logger LOGGER = LoggerFactory.getLogger(AddressChecker.class);
  private final Set<InetAddress> allLocalAddresses;
  private final static int NTTL;

  static {
    String nTTLProp = java.security.Security.getProperty("networkaddress.cache.negative.ttl");
    if (nTTLProp == null) {
      nTTLProp = "10";
    }
    NTTL = Integer.parseInt(nTTLProp);
  }

  public AddressChecker() {
    allLocalAddresses = findAllLocalAddresses();
  }

  public Set<InetAddress> getAllLocalAddresses() {
    return allLocalAddresses;
  }

  public static InetAddress getByName(String hostname, int retry) throws UnknownHostException {
    for (int x=0;x<=retry;x++) {
      try {
        return InetAddress.getByName(hostname);
      } catch (UnknownHostException unknown) {
//  ignore
        if (x != retry) {
          LOGGER.warn("Unable to resolve the hostname provided, waiting for {} seconds and retrying", NTTL);
          ThreadUtil.reallySleep(NTTL * 1000);
        }
      }
    }
    throw new UnknownHostException(hostname);
  }

  public boolean isLegalBindAddress(InetAddress bindAddress) {
    if (bindAddress.isAnyLocalAddress()) { return true; }
    if (bindAddress.isLoopbackAddress()) { return true; }
    return allLocalAddresses.contains(bindAddress);
  }

  private Set<InetAddress> findAllLocalAddresses() {
    Set<InetAddress> rv = new HashSet<InetAddress>();

    final Enumeration<NetworkInterface> nics;
    try {
      nics = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      throw new RuntimeException(e);
    }

    while (nics.hasMoreElements()) {
      NetworkInterface nic = nics.nextElement();
      Enumeration<InetAddress> ips = nic.getInetAddresses();
      while (ips.hasMoreElements()) {
        rv.add(ips.nextElement());
      }
    }

    return Collections.unmodifiableSet(rv);
  }

}
