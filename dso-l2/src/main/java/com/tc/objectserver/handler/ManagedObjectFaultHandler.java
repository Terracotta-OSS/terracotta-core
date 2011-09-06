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
import com.tc.objectserver.core.api.ManagedObjectState;
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
  private final SampledCounter      time2FaultFromStore;
  private final SampledCounter      time2Add2ObjMgr;

  public ManagedObjectFaultHandler(SampledCounter time2FaultFromDiskOrOffheap, SampledCounter time2Add2ObjMgr,
                                   ObjectStatsRecorder objectStatsRecorder) {
    this.objectStatsRecorder = objectStatsRecorder;
    this.time2FaultFromStore = time2FaultFromDiskOrOffheap;
    this.time2Add2ObjMgr = time2Add2ObjMgr;
    this.faultsCounter = new AtomicLong();
  }

  @Override
  public void handleEvent(EventContext context) {
    ManagedObjectFaultingContext mfc = (ManagedObjectFaultingContext) context;
    ObjectID oid = mfc.getId();
    long t0 = System.nanoTime();
    ManagedObject mo = this.objectStore.getObjectByID(oid);
    long t1 = System.nanoTime();
    this.objectManager.addFaultedObject(oid, mo, mfc.isRemoveOnRelease());
    long t2 = System.nanoTime();
    if (LOG_OBJECT_FAULT) {
      // TODO:: Now that this is promoted into an SRA, this should be on all the time. Once SampledCounter is updated to
      // use CAS change this to always sample faults and deprecate the TC property
      logStats(t1 - t0, t2 - t1);
    }
    if (this.objectStatsRecorder.getFaultDebug()) {
      // XXX:: We are accessing ManagedObject after checking it back into ObjectManager. Its is OK to get ClassName
      // since it wont change but not a good idea to inspect any other state.
      String className = getClassName(mo);
      updateStats(className);
    }
  }

  private String getClassName(ManagedObject mo) {
    if (mo != null) {
      ManagedObjectState state = mo.getManagedObjectState();
      if (state != null) { return state.getClassName(); }
    }
    return "UNKNOWN";
  }

  private void updateStats(String className) {
    this.objectStatsRecorder.updateFaultStats(className);
  }

  private void logStats(long time2Fault, long time2Add) {
    this.time2FaultFromStore.increment(time2Fault);
    this.time2Add2ObjMgr.increment(time2Add);
    long count = this.faultsCounter.incrementAndGet();
    // this could be from offheap or disk
    if (count % 1000 == 0) {
      logger.info("Number of Objects faulted = " + count);
    }
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.objectManager = oscc.getObjectManager();
    this.objectStore = oscc.getObjectStore();
  }

}
