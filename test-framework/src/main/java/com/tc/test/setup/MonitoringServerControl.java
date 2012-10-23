package com.tc.test.setup;

import com.tc.objectserver.control.ServerControl;

/**
 * @author tim
 */
public class MonitoringServerControl implements ServerControl {

  private final ServerControl serverControl;
  private final MonitoringServerControlExitCallback exitCallback;

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
  public void startWithoutWait() throws Exception {
    serverControl.startWithoutWait();
    startMonitoring();
  }

  @Override
  public int waitFor() throws Exception {
    return serverControl.waitFor();
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
  public int getDsoPort() {
    return serverControl.getDsoPort();
  }

  @Override
  public int getAdminPort() {
    return serverControl.getAdminPort();
  }

  private void startMonitoring() {
    if (monitoringThread != null) {
      throw new AssertionError("Monitoring is already started.");
    }
    monitoringThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (exitCallback.onExit(serverControl.waitFor())) {
            serverControl.start();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    monitoringThread.setDaemon(true);
    monitoringThread.start();
  }

  private void stopMonitoring() throws InterruptedException {
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
}
