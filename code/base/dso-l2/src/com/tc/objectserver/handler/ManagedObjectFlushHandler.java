/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ManagedObjectFlushingContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

import java.util.List;

public class ManagedObjectFlushHandler extends AbstractEventHandler {

  private ObjectManager objectManager;

  public void handleEvent(EventContext context) {
    ManagedObjectFlushingContext mfc = (ManagedObjectFlushingContext) context;
    List objects2Flush = mfc.getObjectToFlush();
    objectManager.flushAndEvict(objects2Flush);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    objectManager = oscc.getObjectManager();
  }

}
