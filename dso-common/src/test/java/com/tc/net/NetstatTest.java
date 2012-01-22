/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import com.tc.net.Netstat.SocketConnection;

import java.util.List;

import junit.framework.TestCase;

public class NetstatTest extends TestCase {

  public void testBasic() {
    {
      // linux
      Netstat netstat = new Netstat() {
        @Override
        String executeNetstat() throws Exception {
          return "tcp        0      0 10.2.0.108:52464        76.14.32.185:22         ESTABLISHED\n";
        }
      };
      List<SocketConnection> connections = netstat.listEstablishedTcpConnections();
      assertEquals(1, connections.size());
      SocketConnection conn = connections.iterator().next();
      assertEquals("10.2.0.108", conn.getLocalAddr());
      assertEquals(52464, conn.getLocalPort());
      assertEquals("76.14.32.185", conn.getRemoteAddr());
      assertEquals(22, conn.getRemotePort());
    }

    {
      // linux IPv6
      Netstat netstat = new Netstat() {
        @Override
        String executeNetstat() throws Exception {
          return "tcp        0      0 ::ffff:127.0.0.1:19977      ::ffff:127.0.0.1:54047      ESTABLISHED\n";
        }
      };
      List<SocketConnection> connections = netstat.listEstablishedTcpConnections();
      assertEquals(1, connections.size());
      SocketConnection conn = connections.iterator().next();
      assertEquals("127.0.0.1", conn.getLocalAddr());
      assertEquals(19977, conn.getLocalPort());
      assertEquals("127.0.0.1", conn.getRemoteAddr());
      assertEquals(54047, conn.getRemotePort());
    }

    {
      // windows
      Netstat netstat = new Netstat() {
        @Override
        String executeNetstat() throws Exception {
          return "  TCP    10.2.0.107:53628       10.2.0.108:445         ESTABLISHED\n";
        }
      };
      List<SocketConnection> connections = netstat.listEstablishedTcpConnections();
      assertEquals(1, connections.size());
      SocketConnection conn = connections.iterator().next();
      assertEquals("10.2.0.107", conn.getLocalAddr());
      assertEquals(53628, conn.getLocalPort());
      assertEquals("10.2.0.108", conn.getRemoteAddr());
      assertEquals(445, conn.getRemotePort());
    }

    {
      // osx
      Netstat netstat = new Netstat() {
        @Override
        String executeNetstat() throws Exception {
          return "tcp4       0     52  192.168.1.10.22        12.116.106.202.50590   ESTABLISHED\n";
        }
      };
      List<SocketConnection> connections = netstat.listEstablishedTcpConnections();
      assertEquals(1, connections.size());
      SocketConnection conn = connections.iterator().next();
      assertEquals("192.168.1.10", conn.getLocalAddr());
      assertEquals(22, conn.getLocalPort());
      assertEquals("12.116.106.202", conn.getRemoteAddr());
      assertEquals(50590, conn.getRemotePort());
    }

    {
      // solaris
      Netstat netstat = new Netstat() {
        @Override
        String executeNetstat() throws Exception {
          return "127.0.0.1.39969      127.0.0.1.4624       49152      0 49152      0 ESTABLISHED\n";
        }
      };
      List<SocketConnection> connections = netstat.listEstablishedTcpConnections();
      assertEquals(1, connections.size());
      SocketConnection conn = connections.iterator().next();
      assertEquals("127.0.0.1", conn.getLocalAddr());
      assertEquals(39969, conn.getLocalPort());
      assertEquals("127.0.0.1", conn.getRemoteAddr());
      assertEquals(4624, conn.getRemotePort());
    }

  }
}
