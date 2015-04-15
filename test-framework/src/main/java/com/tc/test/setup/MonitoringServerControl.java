/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.test.setup;

import com.tc.objectserver.control.ServerControl;

/**
 * @author tim
 */
public class MonitoringServerControl implements ServerControl {

  private final ServerControl serverControl;
  private final MonitoringServerControlExitCallback exitCallback;

  private volatile int exitCode = -1;

  private Thread monitoringThread;

  public MonitoringServerControl(final ServerControl serverControl, final MonitoringServerControlExitCallback exitCallback) {
    this.serverControl = serverControl;
    this.exitCallback = exitCallback;
  }

  @Override
  public void mergeSTDOUT() {
    serverControl.mergeSTDOUT();
  }

  @Override
  public void mergeSTDERR() {
    serverControl.mergeSTDERR();
  }

  @Override
  public synchronized void attemptForceShutdown() throws Exception {
    stopMonitoring();
    serverControl.attemptForceShutdown();
  }

  @Override
  public synchronized void shutdown() throws Exception {
    stopMonitoring();
    serverControl.shutdown();
  }

  @Override
  public synchronized void crash() throws Exception {
    stopMonitoring();
    serverControl.crash();
  }

  @Override
  public synchronized void start() throws Exception {
    serverControl.start();
    startMonitoring();
  }

  @Override
  public synchronized void startWithoutWait() throws Exception {
    serverControl.startWithoutWait();
    startMonitoring();
  }

  @Override
  public synchronized int waitFor() throws Exception {
    if (monitoringThread != null) {
      monitoringThread.join();
    }
    return exitCode;
  }

  @Override
  public boolean isRunning() {
    return serverControl.isRunning();
  }

  @Override
  public void waitUntilShutdown() throws Exception {
    serverControl.waitUntilShutdown();
  }

  @Override
  public int getTsaPort() {
    return serverControl.getTsaPort();
  }

  @Override
  public int getAdminPort() {
    return serverControl.getAdminPort();
  }

  private synchronized void startMonitoring() throws InterruptedException {
    if (monitoringThread != null) {
      monitoringThread.join();
    }
    monitoringThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (exitCallback.onExit(exitCode = serverControl.waitFor())) {
            serverControl.start();
          }
        } catch (InterruptedException e) {
          // Ignore interrupted exception, it comes on shutdown.
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    monitoringThread.setDaemon(true);
    monitoringThread.start();
  }

  private synchronized void stopMonitoring() throws InterruptedException {
    if (monitoringThread == null) {
      throw new AssertionError("Monitoring was not started.");
    }
    monitoringThread.interrupt();
    monitoringThread.join();
    monitoringThread = null;
  }

  @Override
  public void waitUntilL2IsActiveOrPassive() throws Exception {
    serverControl.waitUntilL2IsActiveOrPassive();
  }

  public static interface MonitoringServerControlExitCallback {
    boolean onExit(int exitCode);
  }

  @Override
  public void pauseServer(long pauseTimeMillis) throws InterruptedException {
    serverControl.pauseServer(pauseTimeMillis);

  }

  @Override
  public void pauseServer() throws InterruptedException {
    serverControl.pauseServer();
  }

  @Override
  public void unpauseServer() throws InterruptedException {
    serverControl.unpauseServer();
  }
}
