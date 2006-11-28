/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;

import javax.management.NotCompliantMBeanException;

public class ObjectManagementMonitor extends AbstractTerracottaMBean implements ObjectManagementMonitorMBean {

  private static final TCLogger logger        = TCLogging.getLogger(ObjectManagementMonitor.class);
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();
  private static final boolean  isDebug       = false;

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
          if (isRunning) { return; }
          isRunning = true;
          consoleLogger.info("Running GC.");
        }
        synchronized (gcControllerLock) {
          gcController.startGC();
        }
        synchronized (this) {
          isRunning = false;
          consoleLogger.info("GC finished.");
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
    if (!isEnabled()) {
      logger.warn("Cannot run GC because mBean is not enabled.");
      return;
    }
    if (isGCRunning()) {
      logger.warn("Cannot run GC because it is already running.");
      return;
    }
    synchronized (gcControllerLock) {
      if (gcController == null) {
        if (isDebug) {
          logger.error("Cannot run GC because gc-controller was not set");
        }
        return;
      }
      new Thread(gcRunner).start();
    }
  }

  public synchronized void reset() {
    // nothing to reset
  }

  public synchronized void registerGCController(GCComptroller controller) {
    if (isEnabled()) {
      synchronized (gcControllerLock) {
        if (gcController != null) {
          if (isDebug) {
            logger.warn("Not registering new gc-controller because one was already registered.");
          }
          return;
        }
        gcController = controller;
      }
    }
  }

  public static interface GCComptroller {
    void startGC();
  }

  static interface GCRunner extends Runnable {
    boolean isGCRunning();
  }
}
