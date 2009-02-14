/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.management.beans.object.ObjectManagementMonitor.GCComptroller;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.l1.api.ClientStateManager;

public class GCComptrollerImpl implements GCComptroller {

  private final GarbageCollector collector;
  private final ObjectManager objectManager;
  private final ClientStateManager stateManager;

  public GCComptrollerImpl(GarbageCollector collector, ObjectManager objectManager, ClientStateManager stateManager) {
    this.collector = collector;
    this.objectManager = objectManager;
    this.stateManager = stateManager;
  }

  public boolean isGCStarted() {
    return collector.isStarted();
  }

  public boolean isGCDisabled() {
    return collector.isDisabled();
  }

  public void startGC() {
    collector.doGC(new FullGCHook(collector, objectManager, stateManager));
  }

}
