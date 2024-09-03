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
            s.close();
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
