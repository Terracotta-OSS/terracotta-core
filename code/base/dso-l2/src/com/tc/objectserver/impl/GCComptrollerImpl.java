/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.management.beans.object.ObjectManagementMonitor.GCComptroller;
import com.tc.objectserver.core.api.GarbageCollector;

public class GCComptrollerImpl implements GCComptroller {

  private final ObjectManagerConfig config;
  private final GarbageCollector collector;

  public GCComptrollerImpl(ObjectManagerConfig config, GarbageCollector collector) {
    this.config = config;
    this.collector = collector;
  }

  public boolean gcEnabledInConfig() {
    return config.doGC();
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
