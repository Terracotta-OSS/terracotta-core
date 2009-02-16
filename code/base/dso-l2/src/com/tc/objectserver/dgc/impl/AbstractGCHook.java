/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.l1.api.ClientStateManager;

public abstract class AbstractGCHook implements GCHook {
  protected final MarkAndSweepGarbageCollector collector;
  protected final ObjectManager                objectManager;
  protected final ClientStateManager           stateManager;

  protected AbstractGCHook(MarkAndSweepGarbageCollector collector, ObjectManager objectManager,
                           ClientStateManager stateManager) {
    this.collector = collector;
    this.objectManager = objectManager;
    this.stateManager = stateManager;
  }

  public void startMonitoringReferenceChanges() {
    this.collector.startMonitoringReferenceChanges();
  }

  public void stopMonitoringReferenceChanges() {
    this.collector.stopMonitoringReferenceChanges();
  }

  public void waitUntilReadyToGC() {
    this.objectManager.waitUntilReadyToGC();
  }
}