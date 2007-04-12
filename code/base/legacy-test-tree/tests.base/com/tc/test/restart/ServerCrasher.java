/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.restart;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.exception.TCRuntimeException;
import com.tc.objectserver.control.ServerControl;
import com.tc.util.concurrent.ThreadUtil;

public class ServerCrasher implements Runnable {
  private final ServerControl       server;
  private final Thread              myThread  = new Thread(this, "ServerCrasher");
  private final long                crashInterval;
  private final boolean             crash;
  private final SynchronizedBoolean isRunning = new SynchronizedBoolean(false);

  public ServerCrasher(final ServerControl server, final long crashInterval, final boolean crash) {
    super();
    this.server = server;
    this.crashInterval = crashInterval;
    this.crash = crash;
  }

  public void startAutocrash() throws Exception {
    isRunning.set(true);
    myThread.start();
  }

  public void run() {
    try {
      while (isRunning.get()) {
        System.err.println("Starting server...");
        synchronized (isRunning) {
          if (isRunning.get()) server.start(30 * 1000);
        }
        ThreadUtil.reallySleep(crashInterval);
        synchronized (isRunning) {
          if (isRunning.get()) {
            if (crash) {
              System.err.println("Crashing server...");
              server.crash();
            } else {
              System.err.println("Shutting server down...");
              server.shutdown();
            }
            if (server.isRunning()) throw new AssertionError("Server is still running even after shutdown or crash.");
          }
        }
      }
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  public void stop() throws Exception {
    synchronized (isRunning) {
      isRunning.set(false);
      server.shutdown();
    }
  }
}
