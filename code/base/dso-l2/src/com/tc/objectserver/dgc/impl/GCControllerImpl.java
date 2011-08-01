/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.management.beans.object.ObjectManagementMonitor.GCController;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;

public class GCControllerImpl implements GCController {

  private final GarbageCollector collector;

  public GCControllerImpl(GarbageCollector collector) {
    this.collector = collector;
  }

  public boolean isGCStarted() {
    return this.collector.isStarted();
  }

  public boolean isGCDisabled() {
    return this.collector.isDisabled();
  }

  public void startGC() {
    this.collector.doGC(GCType.FULL_GC);
  }

}
