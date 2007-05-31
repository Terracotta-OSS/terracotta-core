/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.proxyconnect;

import com.tc.net.proxy.TCPProxy;
import com.tc.util.PortChooser;

import java.io.File;
import java.net.InetAddress;
import java.lang.RuntimeException;

public class ProxyConnectManagerImpl implements ProxyConnectManager {
  private TCPProxy proxy = null;
  private int proxyPort, dsoPort;
  private static ProxyConnectManagerImpl mgr = null;
  private int downtime = 100;
  private int waittime = 20 * 1000;
  
  private ProxyConnectManagerImpl() {
    PortChooser pc = new PortChooser();
    proxyPort = pc.chooseRandomPort();
    dsoPort = pc.chooseRandomPort();
  }
  
  public static ProxyConnectManagerImpl getManager() {
    if (mgr == null) mgr = new ProxyConnectManagerImpl();
    return(mgr);
  }
  
  public void setupProxy() {
    try {
      proxy = new TCPProxy(proxyPort, InetAddress.getLocalHost(), dsoPort, 0L, false, new File("."));
      proxy.setReuseAddress(true);
    } catch (Exception x) {
      throw new RuntimeException("setupProxy failed! "+x);
    }
  }
  
  public void setProxyPort(int port) {
    proxyPort = port;
  }
  
  public int getProxyPort() {
    return(proxyPort);
  }
  
  public void setDsoPort(int port) {
    dsoPort = port;
  }
  
  public int getDsoPort() {
    return(dsoPort);
  }

  public void proxyDown() {
    System.out.println("XXX stop proxy");
    proxy.stop();
  }

  public void proxyUp(){
    try {
      System.out.println("XXX start proxy at "+proxyPort+" to "+ dsoPort);
      proxy.start();
    } catch (Exception x) {
      throw new RuntimeException("proxyUp failed! "+x);
    }
  }
  
  public void startProxyTest() {
    Thread td = new Thread(new Runnable() {
      public void run() {
        runProxy();
      }
    });
    td.start();
  }
  
  private void runProxy() {
    proxyUp();
    
    try {
      Thread.sleep(waittime);
    } catch (Exception x) {
      throw new RuntimeException("proxy wait time failed! " + x);
      //return;
    }
    
    proxyDown();
    
    try {
      Thread.sleep(downtime);
    } catch (Exception x) {
      throw new RuntimeException("proxy down time failed! " + x);
      //return;
    }

    proxyUp();
  }

  public void setProxyDownTime(int milliseconds) {
    downtime = milliseconds;
  }
  
  public int getProxyDownTime() {
    return(downtime);
  }

  public void setProxyWaitTime(int milliseconds) {
    waittime = milliseconds;
  }
  
  public int getProxyWaitTime() {
    return(waittime);
  }

}
