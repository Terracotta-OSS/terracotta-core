/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.restart;

import com.tc.exception.TCRuntimeException;
import com.tc.objectserver.control.ServerControl;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.TestState;

public class ServerCrasher implements Runnable {
  private final ServerControl       server;
  private final Thread              myThread         = new Thread(this, "ServerCrasher");
  private final long                crashInterval;
  private final TestState           testState;
  private boolean                   proxyConnectMode = false;
  private final ProxyConnectManager proxyMgr;

  public ServerCrasher(final ServerControl server, final long crashInterval, final boolean crash, TestState testState,
                       ProxyConnectManager proxyMgr) {
    super();
    this.server = server;
    this.crashInterval = crashInterval;
    this.testState = testState;
    this.proxyMgr = proxyMgr;
  }

  public void startAutocrash() throws Exception {
    testState.setTestState(TestState.RUNNING);
    myThread.start();
  }

  public void setProxyConnectMode(boolean onoff) {
    proxyConnectMode = onoff;
  }

  private void startServer() throws Exception {
    System.err.println("Starting server...");
    server.start();
    if (proxyConnectMode) {
      proxyMgr.proxyUp();
      proxyMgr.startProxyTest();
    }
  }

  private void crashServer() throws Exception {
    System.err.println("Crashing server...");
    if (proxyConnectMode) {
      proxyMgr.stopProxyTest();
      proxyMgr.proxyDown();
    }
    server.crash();
  }

  public void run() {
    // initial server start
    try {
      synchronized (testState) {
        if (testState.isRunning()) {
          startServer();
        }
      }
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }

    while (true) {
      ThreadUtil.reallySleep(crashInterval);
      synchronized (testState) {
        if (testState.isRunning()) {
          try {
            crashServer();
            if (server.isRunning()) throw new AssertionError("Server is still running even after shutdown or crash.");
            startServer();
          } catch (Exception e) {
            throw new TCRuntimeException(e);
          }
        } else {
          System.err.println("Shutting down server crasher.");
          break;
        }
      }
    }
  }
}
