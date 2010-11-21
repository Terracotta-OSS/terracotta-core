/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.proxyconnect;

import com.tc.net.proxy.TCPProxy;
import com.tc.util.PortChooser;

import java.io.File;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProxyConnectManagerImpl implements ProxyConnectManager {
  private TCPProxy         proxy    = null;
  private int              proxyPort, dsoPort;
  private int              downtime = 100;
  private int              waittime = 20 * 1000;
  private Thread           td       = null;
  private volatile boolean toStop   = false;

  public ProxyConnectManagerImpl() {
    PortChooser pc = new PortChooser();
    proxyPort = pc.chooseRandomPort();
    dsoPort = pc.chooseRandomPort();
  }

  public void setupProxy() {
    try {
      proxy = new TCPProxy(proxyPort, InetAddress.getByName("localhost"), dsoPort, 0L, false, new File("."));
      proxy.setReuseAddress(true);
    } catch (Exception x) {
      throw new RuntimeException("setupProxy failed! " + x);
    }
  }

  public void setProxyPort(int port) {
    proxyPort = port;
  }

  public int getProxyPort() {
    return (proxyPort);
  }

  public void setDsoPort(int port) {
    dsoPort = port;
  }

  public int getDsoPort() {
    return (dsoPort);
  }

  private String timeStamp() {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    return (formatter.format(new Date()));
  }

  public void proxyDown() {
    System.out.println("XXX " + timeStamp() + " stop proxy");
    proxy.fastStop();
  }

  public void proxyUp() {
    // wait until backend is ready
    int i = 0;
    while (!proxy.probeBackendConnection()) {
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        //
      }
      if (i++ > 1200) { throw new RuntimeException("L2 is not ready!"); }
    }

    try {
      System.out.println("XXX " + timeStamp() + " start proxy at " + proxyPort + " to " + dsoPort);
      proxy.start();
    } catch (Exception x) {
      throw new RuntimeException("proxyUp failed! " + x);
    }
  }

  public void stopProxyTest() {
    toStop = true;
    proxyDown();
    if (td.isAlive()) td.interrupt();
    while (td.isAlive()) {
      try {
        Thread.sleep(10);
      } catch (Exception x) {
        //
      }
      if (td.isAlive()) td.interrupt();
    }
    System.out.println("XXX " + timeStamp() + " proxy thread stopped");
  }

  public void startProxyTest() {
    toStop = false;
    td = new Thread(new Runnable() {
      public void run() {
        runProxy();
      }
    });
    td.start();
  }

  private void runProxy() {
    if (toStop) return;

    try {
      Thread.sleep(waittime);
    } catch (Exception x) {
      // throw new RuntimeException("proxy wait time failed! " + x);
      // return;
    }
    if (toStop) return;
    proxyDown();

    try {
      Thread.sleep(downtime);
    } catch (Exception x) {
      // throw new RuntimeException("proxy down time failed! " + x);
      // return;
    }
    if (toStop) return;

    proxyUp();
  }

  public void setProxyDownTime(int milliseconds) {
    downtime = milliseconds;
  }

  public int getProxyDownTime() {
    return (downtime);
  }

  public void setProxyWaitTime(int milliseconds) {
    waittime = milliseconds;
  }

  public int getProxyWaitTime() {
    return (waittime);
  }

  public void close() {
    this.proxy.stop();
  }

  public void closeClientConnections() {
    this.proxy.closeClientConnections(true, true);
  }

}
