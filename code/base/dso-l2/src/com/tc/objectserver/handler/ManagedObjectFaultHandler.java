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
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.SampledCounter;

import java.util.concurrent.atomic.AtomicLong;

public class ManagedObjectFaultHandler extends AbstractEventHandler {

  private static final TCLogger     logger           = TCLogging.getLogger(ManagedObjectFaultHandler.class);
  private static final boolean      LOG_OBJECT_FAULT = TCPropertiesImpl
                                                         .getProperties()
                                                         .getBoolean(
                                                                     TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED);

  private ObjectManager             objectManager;
  private ManagedObjectStore        objectStore;
  private final ObjectStatsRecorder objectStatsRecorder;
  // TODO::Remove this counter, its not needed if you remove the logging @see below
  private final AtomicLong          faultsCounter;
  private final SampledCounter      faultFromDisk;
  private final SampledCounter      time2FaultFromDisk;
  private final SampledCounter      time2Add2ObjMgr;

  public ManagedObjectFaultHandler(SampledCounter faultFromDisk, SampledCounter time2FaultFromDisk,
                                   SampledCounter time2Add2ObjMgr, ObjectStatsRecorder objectStatsRecorder) {
    this.faultFromDisk = faultFromDisk;
    this.objectStatsRecorder = objectStatsRecorder;
    this.time2FaultFromDisk = time2FaultFromDisk;
    this.time2Add2ObjMgr = time2Add2ObjMgr;
    this.faultsCounter = new AtomicLong();
  }

  public void handleEvent(EventContext context) {
    ManagedObjectFaultingContext mfc = (ManagedObjectFaultingContext) context;
    ObjectID oid = mfc.getId();
    long t0 = System.nanoTime();
    ManagedObject mo = objectStore.getObjectByID(oid);
    long t1 = System.nanoTime();
    objectManager.addFaultedObject(oid, mo, mfc.isRemoveOnRelease());
    long t2 = System.nanoTime();
    if (LOG_OBJECT_FAULT) {
      // TODO:: Now that this is promoted into an SRA, this should be on all the time. Once SampledCounter is updated to
      // use CAS change this to always sample faults and deprecate the TC property
      logStats(t1 - t0, t2 - t1);
    }
    if (objectStatsRecorder.getFaultDebug()) {
      updateStats(mo);
    }
  }

  private void updateStats(ManagedObject mo) {
    String className = mo.getManagedObjectState().getClassName();
    if (className == null) className = "UNKNOWN"; // Could happen on restart scenario
    objectStatsRecorder.updateFaultStats(className);
  }

  private void logStats(long time2Fault, long time2Add) {
    faultFromDisk.increment();
    time2FaultFromDisk.increment(time2Fault);
    time2Add2ObjMgr.increment(time2Add);
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
