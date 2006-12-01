/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.network;

import com.tc.net.proxy.TCPProxy;
import com.tc.util.Assert;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public final class TCPProxyFixture {

  private final TCPProxy proxy;
  private final int      localPort;
  private boolean        up;

  /**
   * There is a bit of a race condition here where someone can use our local socket in between us stopping and starting --
   * but that's just too bad and a risk we will have to take.
   */
  public TCPProxyFixture(String remoteHost, int remotePort) throws UnknownHostException {
    Assert.assertNotNull(remoteHost);
    localPort = findAvailableLocalPort();
    proxy = new TCPProxy(localPort, InetAddress.getByName(remoteHost), remotePort, 0, false, null);
    up = false;
  }

  public int localPort() {
    return localPort;
  }

  public synchronized void severeConnection() {
    Assert.eval("Connection must be started in order to severe it", up);
    proxy.stop();
    up = false;
  }

  public synchronized void establishConnection() throws IOException {
    Assert.eval("Connection must be stopped to start it", !up);
    proxy.start();
    up = true;
  }

  private static int findAvailableLocalPort() {
    int port = 18456;
    while (true) {
      Socket socket = new Socket();
      try {
        socket.connect(new InetSocketAddress(port));
        // Already being used, try the next port
        socket.close();
        ++port;
      } catch (IOException e) {
        // Found an unused port
        break;
      }
    }
    return port;
  }

}
