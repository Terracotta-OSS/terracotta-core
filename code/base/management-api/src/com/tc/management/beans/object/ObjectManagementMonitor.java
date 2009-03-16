/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.management.NotCompliantMBeanException;

public class ObjectManagementMonitor extends AbstractTerracottaMBean implements ObjectManagementMonitorMBean {

  private static final TCLogger  logger = TCLogging.getLogger(ObjectManagementMonitor.class);

  private volatile GCComptroller gcController;

  private final GCRunner         gcRunner;
  private ObjectIdsFetcher       objectIdsFetcher;

  public ObjectManagementMonitor() throws NotCompliantMBeanException {
    super(ObjectManagementMonitorMBean.class, false);

    gcRunner = new GCRunner() {
      private boolean isRunning = false;

      public void run() {
        setRunningState();
        gcController.startGC();
        setStopState();
      }

      private synchronized void setRunningState() {
        if (isRunning) {
          logger.warn("Cannot run DGC because DGC is already running.");
          return;
        }
        isRunning = true;
        logger.info("Running DGC.");
      }

      private synchronized void setStopState() {
        if (!isRunning) {
          logger.warn("Cannot stop DGC because DGC is not running.");
          return;
        }
        isRunning = false;
        logger.info("DGC finished.");
      }

      public synchronized boolean isGCRunning() {
        return isRunning;
      }
    };
  }

  public boolean isGCRunning() {
    return gcRunner.isGCRunning();
  }

  public synchronized boolean runGC() {
    if (!isEnabled()) {
      logger.warn("Cannot run DGC because mBean is not enabled.");
    }
    if (gcController == null) { throw new RuntimeException("Failure: see log for more information"); }

    if (!gcController.isGCStarted()) {
      logger.warn("Cannot run DGC externally because this server " + "is in PASSIVE state and DGC is disabled.");
    }
    // XXX::Note:: There is potencially a rare here, one could check to see
    // if it is disabled and before DGC is started it could be disabled. In
    // which case it will not be run, just logged in the log file.
    if (gcController.isGCDisabled()) { throw new UnsupportedOperationException(
                                                                               "Cannot run DGC externally because PASSIVE server(s) are currently synching state "
                                                                                   + "with this ACTIVE server and DGC is disabled."); }
    if (isGCRunning()) {
      logger.warn("Cannot run DGC because DGC is already running.");
    }

    if (!gcController.isGCStarted()) {
      logger.warn("Cannot run DGC externally because this server " + "is in PASSIVE state and DGC is disabled.");
      return false;
    }
    // XXX::Note:: There is potencially a rare here, one could check to see
    // if it is disabled and before DGC is started it could be disabled. In
    // which case it will not be run, just logged in the log file.
    if (gcController.isGCDisabled()) {
      logger.warn("Cannot run DGC externally because PASSIVE server(s) are currently synching state "
                  + "with this ACTIVE server and DGC is disabled.");
      return false;
    }
    if (isGCRunning()) {
      logger.warn("Cannot run DGC because DGC is already running.");
      return false;
    }

    Thread gcRunnerThread = new Thread(gcRunner);
    gcRunnerThread.setName("DGCRunnerThread");
    gcRunnerThread.start();
    
    return true;
  }

  public synchronized void reset() {
    // nothing to reset
  }

  public void registerGCController(GCComptroller controller) {
    if (isEnabled()) {
      if (gcController != null) {
        logger.warn("Registering new dgc-controller while one already registered. Old : " + gcController);
      }
      gcController = controller;
    }
  }

  public void registerObjectIdFetcher(ObjectIdsFetcher fetcher) {
    objectIdsFetcher = fetcher;
  }

  public SortedSet getAllObjectIds() {
    Set set = objectIdsFetcher.getAllObjectIds();
    SortedSet treeSet = new TreeSet();
    for (Iterator iter = set.iterator(); iter.hasNext();) {
      treeSet.add(iter.next());
    }
    return treeSet;
  }

  public static interface GCComptroller {
    void startGC();

    boolean isGCDisabled();

    boolean isGCStarted();
  }

  public static interface ObjectIdsFetcher {
    Set getAllObjectIds();
  }

  static interface GCRunner extends Runnable {
    boolean isGCRunning();
  }
}
