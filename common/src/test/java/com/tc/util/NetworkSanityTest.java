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
package com.tc.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.TestCase;

/**
 * Make sure that local hostname and basic networking for it are configured correctly.
 */
public class NetworkSanityTest extends TestCase {

  public void testSanity() throws Exception {
    String host = InetAddress.getLocalHost().getHostName();
    InetAddress addr = InetAddress.getByName(host);
    System.err.println("I think my hostname is " + host + " which resolves to " + addr.getHostAddress());

    NetworkInterface nic = NetworkInterface.getByInetAddress(addr);
    if (nic == null) { throw new AssertionError("No network interface present for " + addr.getHostAddress()); }

    ServerSocket ss = new ServerSocket(0, 50, addr);
    System.err.println("Starting listener at " + ss.getLocalSocketAddress());
    accept(ss);
    try {
      Socket s = new Socket(addr, ss.getLocalPort());
      s.close();

      s = new Socket(host, ss.getLocalPort());
      s.close();
    } finally {
      try {
        ss.close();
      } catch (IOException ioe) {
        //
      }
    }
  }

  private static void accept(final ServerSocket ss) {
    Thread t = new Thread() {
      @Override
      public void run() {
        while (true) {
          Socket s;
          try {
            s = ss.accept();
          } catch (IOException e) {
            return;
          }

          try {
            s.close();
          } catch (IOException e) {
            //
          }
        }
      }
    };
    t.setDaemon(true);
    t.start();
  }
}
