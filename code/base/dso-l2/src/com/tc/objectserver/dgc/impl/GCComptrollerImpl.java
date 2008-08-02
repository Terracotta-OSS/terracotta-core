/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.management.beans.object.ObjectManagementMonitor.GCComptroller;
import com.tc.objectserver.dgc.api.GarbageCollector;

public class GCComptrollerImpl implements GCComptroller {

  private final GarbageCollector collector;

  public GCComptrollerImpl(GarbageCollector collector) {
    this.collector = collector;
  }

  public boolean isGCStarted() {
    return collector.isStarted();
  }

  public boolean isGCDisabled() {
    return collector.isDisabled();
  }

  public void startGC() {
    collector.gc();
  }

}
