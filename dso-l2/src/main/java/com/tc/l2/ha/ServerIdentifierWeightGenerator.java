/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
  public long getWeight() {
    return id;
  }

}
