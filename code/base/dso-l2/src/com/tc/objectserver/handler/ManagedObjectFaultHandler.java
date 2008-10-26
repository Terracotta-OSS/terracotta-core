/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.SampledCounter;

import java.util.concurrent.atomic.AtomicLong;

public class ManagedObjectFaultHandler extends AbstractEventHandler {

  private static final TCLogger logger           = TCLogging.getLogger(ManagedObjectFaultHandler.class);
  private static final boolean  LOG_OBJECT_FAULT = TCPropertiesImpl
                                                     .getProperties()
                                                     .getBoolean(
                                                                 TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED);

  private ObjectManager         objectManager;
  private ManagedObjectStore    objectStore;
  //TODO::Remove this counter, its not needed if you remove the logging @see below
  private final AtomicLong      faultsCounter;
  private final SampledCounter  faultFromDisk;

  public ManagedObjectFaultHandler(SampledCounter faultFromDisk) {
    this.faultFromDisk = faultFromDisk;
    faultsCounter = new AtomicLong();
  }

  public void handleEvent(EventContext context) {
    if (LOG_OBJECT_FAULT) {
      // TODO:: Now that this is promoted into an SRA, this should be on all the time. Once SampledCounter is updated to
      // use CAS change this to always sample faults and deprecate the TC property
      incrementAndLog();
    }
    ManagedObjectFaultingContext mfc = (ManagedObjectFaultingContext) context;
    ObjectID oid = mfc.getId();
    ManagedObject mo = objectStore.getObjectByID(oid);
    objectManager.addFaultedObject(oid, mo, mfc.isRemoveOnRelease());
  }

  private void incrementAndLog() {
    faultFromDisk.increment();
    long count = faultsCounter.incrementAndGet();
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

}
