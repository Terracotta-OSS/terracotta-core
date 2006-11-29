/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;

import javax.management.NotCompliantMBeanException;

public class ObjectManagementMonitor extends AbstractTerracottaMBean implements ObjectManagementMonitorMBean {

  private static final TCLogger logger = TCLogging.getLogger(ObjectManagementMonitor.class);

  private final Object          gcControllerLock;
  private GCComptroller         gcController;
  private final GCRunner        gcRunner;

  public ObjectManagementMonitor() throws NotCompliantMBeanException {
    super(ObjectManagementMonitorMBean.class, false);
    gcControllerLock = new Object();

    gcRunner = new GCRunner() {
      private boolean isRunning = false;

      public void run() {
        synchronized (this) {
          if (isRunning) { throw new UnsupportedOperationException("Cannot run GC because GC is already running."); }
          isRunning = true;
          logger.info("Running GC.");
        }
        synchronized (gcControllerLock) {
          gcController.startGC();
        }
        synchronized (this) {
          isRunning = false;
          logger.info("GC finished.");
        }
      }

      public synchronized boolean isGCRunning() {
        return isRunning;
      }
    };
  }

  public synchronized boolean isGCRunning() {
    return gcRunner.isGCRunning();
  }

  public synchronized void runGC() {
    if (!isEnabled()) { throw new UnsupportedOperationException("Cannot run GC because mBean is not enabled."); }
    synchronized (gcControllerLock) {
      if (gcController == null) { throw new RuntimeException("Failure: see log for more information"); }
      if (gcController.gcEnabledInConfig()) { throw new UnsupportedOperationException(
          "Cannot run GC externally because GC is enabled through config."); }
    }
    if (isGCRunning()) { throw new UnsupportedOperationException("Cannot run GC because GC is already running."); }
    new Thread(gcRunner).start();
  }

  public synchronized void reset() {
    // nothing to reset
  }

  public synchronized void registerGCController(GCComptroller controller) {
    if (isEnabled()) {
      synchronized (gcControllerLock) {
        if (gcController != null) {
          logger.warn("Not registering new gc-controller because one was already registered.");
          return;
        }
        gcController = controller;
      }
    }
  }

  public static interface GCComptroller {
    void startGC();

    boolean gcEnabledInConfig();
  }

  static interface GCRunner extends Runnable {
    boolean isGCRunning();
  }
}
