/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.TCRuntimeException;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;

public class IntraProcessServerControl extends ServerControlBase {
  private boolean                              isStarted = false;
  private TCServer                             server;
  private final L2TVSConfigurationSetupManager configSetupManager;

  public IntraProcessServerControl(L2TVSConfigurationSetupManager configSetupManager, String host) {
    super(host, configSetupManager.dsoL2Config().listenPort().getInt(), configSetupManager.commonl2Config().jmxPort()
        .getInt());
    this.configSetupManager = configSetupManager;
  }

  public void crash() throws Exception {
    throw new UnsupportedOperationException("Can't crash an in-process server.");
  }

  public synchronized void start() throws Exception {
    if (isRunning()) throw new RuntimeException("Server is already running!");
    server = new TCServerImpl(this.configSetupManager);
    server.start();
    isStarted = true;
  }

  public synchronized void shutdown() throws Exception {
    if (!isStarted) return;
    if (server == null) throw new AssertionError("Server is null!");
    server.stop();
    isStarted = false;
  }

  public void attemptShutdown() throws Exception {
    new Thread(getClass().getName() + ".attemptShutdown()") {
      public void run() {
        try {
          shutdown();
        } catch (Exception e) {
          throw new TCRuntimeException(e);
        }
      }
    }.start();
  }

  public void mergeSTDOUT() {
    return;
  }

  public void mergeSTDERR() {
    return;
  }

  public void waitUntilShutdown() throws Exception {
    throw new UnsupportedOperationException();
  }

}
