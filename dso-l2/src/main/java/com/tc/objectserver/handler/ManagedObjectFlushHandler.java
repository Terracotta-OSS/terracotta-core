/*
 * Copyright (c) 2003-2008 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ManagedObjectFlushingContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;

import java.util.Iterator;
import java.util.List;

public class ManagedObjectFlushHandler extends AbstractEventHandler {

  private ObjectManager             objectManager;
  private final ObjectStatsRecorder objectStatsRecorder;

  public ManagedObjectFlushHandler(ObjectStatsRecorder objectStatsRecorder) {
    this.objectStatsRecorder = objectStatsRecorder;
  }

  public void handleEvent(EventContext context) {
    ManagedObjectFlushingContext mfc = (ManagedObjectFlushingContext) context;
    List objects2Flush = mfc.getObjectToFlush();
    objectManager.flushAndEvict(objects2Flush);
    if (objectStatsRecorder.getFlushDebug()) {
      updateStats(objects2Flush);
    }
  }

  private void updateStats(List objects2Flush) {
    Iterator iter = objects2Flush.iterator();
    while (iter.hasNext()) {
      ManagedObject mo = (ManagedObject) iter.next();
      String className = mo.getManagedObjectState().getClassName();
      if (className == null) className = "UNKNOWN";
      objectStatsRecorder.updateFlushStats(className);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    objectManager = oscc.getObjectManager();
  }

}
