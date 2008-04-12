/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.ManagedObjectFaultingContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.properties.TCPropertiesImpl;
import com.tc.properties.TCPropertiesConsts;

import java.util.concurrent.atomic.AtomicLong;

public class ManagedObjectFaultHandler extends AbstractEventHandler {

  private static final TCLogger logger           = TCLogging.getLogger(ManagedObjectFaultHandler.class);
  private static final boolean  LOG_OBJECT_FAULT = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED);

  private ObjectManager         objectManager;
  private ManagedObjectStore    objectStore;
  private AtomicLong            faultCount       = new AtomicLong();

  public void handleEvent(EventContext context) {
    if (LOG_OBJECT_FAULT) incrementAndLog();
    ManagedObjectFaultingContext mfc = (ManagedObjectFaultingContext) context;
    ObjectID oid = mfc.getId();
    ManagedObject mo = objectStore.getObjectByID(oid);
    objectManager.addFaultedObject(oid, mo, mfc.isRemoveOnRelease());
  }

  private void incrementAndLog() {
    long count = faultCount.incrementAndGet();
    if (count % 1000 == 0) {
      logger.info("Number of Objects faulted from disk = " + count);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    objectManager = oscc.getObjectManager();
    objectStore = oscc.getObjectStore();
  }

  public AtomicLong getFaultCount() {
    return faultCount;
  }
}
