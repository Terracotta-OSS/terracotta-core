/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;

import java.util.SortedSet;

public class GarbageDisposeHandler extends AbstractEventHandler {

  private final ManagedObjectPersistor         managedObjectPersistor;
  private final GarbageCollectionInfoPublisher publisher;

  public GarbageDisposeHandler(GarbageCollectionInfoPublisher publisher, ManagedObjectPersistor managedObjectPersistor) {
    this.managedObjectPersistor = managedObjectPersistor;
    this.publisher = publisher;
  }

  @Override
  public void handleEvent(EventContext context) {
    GCResultContext gcResult = (GCResultContext) context;
    GarbageCollectionInfo gcInfo = gcResult.getGCInfo();

    publisher.fireGCDeleteEvent(gcInfo);
    long start = System.currentTimeMillis();
    SortedSet<ObjectID> sortedGarbage = gcResult.getGCedObjectIDs();
    gcInfo.setActualGarbageCount(sortedGarbage.size());
    this.managedObjectPersistor.deleteAllObjects(sortedGarbage);

    long elapsed = System.currentTimeMillis() - start;
    gcInfo.setDeleteStageTime(elapsed);
    long elapsedTime = System.currentTimeMillis() - gcInfo.getStartTime();
    gcInfo.setElapsedTime(elapsedTime);
    gcInfo.setEndObjectCount(managedObjectPersistor.getObjectCount());
    publisher.fireGCCompletedEvent(gcInfo);
  }

}
