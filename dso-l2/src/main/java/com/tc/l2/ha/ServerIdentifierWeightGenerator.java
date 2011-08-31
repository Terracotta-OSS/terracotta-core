/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.exception.TCRuntimeException;
import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class ServerIdentifierWeightGenerator implements WeightGenerator {
  private final long id;

  public ServerIdentifierWeightGenerator(String host, int port) {
    id = convertHostPortToVal(host, port);
  }

  private long convertHostPortToVal(String host, int port) {
    long val = 0;
    byte[] addr = getIpAddressinBytes(host);
    for (byte element : addr) {
      val <<= 8;
      val += element & 0xff;
    }
    val <<= 16;
    val += port;
    return val;
  }

  private byte[] getIpAddressinBytes(String host) {
    try {
      InetAddress ip = InetAddress.getByName(host);
      if (ip.isLoopbackAddress()) {
        InetAddress ip2 = getNotLoopbackIPAddr();
        if (ip2 != null) ip = ip2;
      }
      byte[] addr = ip.getAddress();
      return addr;
    } catch (UnknownHostException e) {
      throw new TCRuntimeException(e);
    }
  }

  private InetAddress getNotLoopbackIPAddr() {
    try {
      for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
        NetworkInterface ni = e.nextElement();
        for (Enumeration<InetAddress> addresses = ni.getInetAddresses(); addresses.hasMoreElements();) {
          InetAddress ip = addresses.nextElement();
          if ((ip instanceof Inet4Address) && !ip.isLoopbackAddress()) { return ip; }
        }
      }
    } catch (SocketException e) {
      throw new TCRuntimeException(e);
    }
    return null;
  }

  public long getWeight() {
    return id;
  }

}
