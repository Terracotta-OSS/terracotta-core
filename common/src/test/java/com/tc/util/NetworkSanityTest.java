/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
