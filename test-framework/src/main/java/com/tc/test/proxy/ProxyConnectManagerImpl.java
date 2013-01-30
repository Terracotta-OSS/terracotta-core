/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.proxy;

import com.tc.net.proxy.TCPProxy;
import com.tc.util.PortChooser;

import java.io.File;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProxyConnectManagerImpl implements ProxyConnectManager {
  private TCPProxy         proxy         = null;
  private int              proxyPort, tsaPort;
  private int              downtime      = 100;
  private int              waittime      = 20 * 1000;
  private Thread           td            = null;
  private volatile boolean toStop        = false;
  private volatile boolean manualControl = false;

  public ProxyConnectManagerImpl() {
    PortChooser pc = new PortChooser();
    proxyPort = pc.chooseRandomPort();
    tsaPort = pc.chooseRandomPort();
  }

  public ProxyConnectManagerImpl(int port, int proxyPort) {
    this.proxyPort = proxyPort;
    this.tsaPort = port;
  }

  @Override
  public void setupProxy() {
    try {
      proxy = new TCPProxy(proxyPort, InetAddress.getByName("localhost"), tsaPort, 0L, false, new File("."));
      proxy.setReuseAddress(true);
    } catch (Exception x) {
      throw new RuntimeException("setupProxy failed! " + x);
    }
  }

  @Override
  public void setProxyPort(int port) {
    proxyPort = port;
  }

  @Override
  public int getProxyPort() {
    return (proxyPort);
  }

  @Override
  public void setTsaPort(int port) {
    tsaPort = port;
  }

  @Override
  public int getTsaPort() {
    return tsaPort;
  }

  @Override
  public void setManualControl(boolean manualControl) {
    System.out.println("XXX " + manualControl + " manual proxy control");
    this.manualControl = manualControl;
  }

  @Override
  public boolean isManualControl() {
    return manualControl;
  }

  private String timeStamp() {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    return (formatter.format(new Date()));
  }

  @Override
  public void proxyDown() {
    System.out.println("XXX " + timeStamp() + " stop proxy");
    proxy.fastStop();
  }

  @Override
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
      System.out.println("XXX " + timeStamp() + " start proxy at " + proxyPort + " to " + tsaPort);
      proxy.start();
    } catch (Exception x) {
      throw new RuntimeException("proxyUp failed! " + x);
    }
  }

  @Override
  public void stopProxyTest() {
    if (isManualControl()) {
      // Nothing to stop in manual control mode
      return;
    }
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

  @Override
  public void startProxyTest() {
    if (isManualControl()) {
      System.out.println("Manual proxy control mode, not starting proxy control thread.");
      return;
    }
    toStop = false;
    td = new Thread(new Runnable() {
      @Override
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

  @Override
  public void setProxyDownTime(int milliseconds) {
    downtime = milliseconds;
  }

  @Override
  public int getProxyDownTime() {
    return (downtime);
  }

  @Override
  public void setProxyWaitTime(int milliseconds) {
    waittime = milliseconds;
  }

  @Override
  public int getProxyWaitTime() {
    return (waittime);
  }

  @Override
  public void close() {
    this.proxy.stop();
  }

  @Override
  public void closeClientConnections() {
    this.proxy.closeClientConnections(true, true);
  }

}
