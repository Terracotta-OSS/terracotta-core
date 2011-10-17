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
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.persistence.api.ManagedObjectStore;

import java.util.SortedSet;

public class GarbageDisposeHandler extends AbstractEventHandler {
  private static final TCLogger                logger = TCLogging.getLogger(GarbageDisposeHandler.class);

  private final GarbageCollectionInfoPublisher publisher;
  private ManagedObjectStore                   objectStore;

  public GarbageDisposeHandler(final GarbageCollectionInfoPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof DGCResultContext) {
      handleDGCResult((DGCResultContext) context);
    } else {
      throw new AssertionError("Unknown context type: " + context.getClass().getName());
    }
  }

  private void handleDGCResult(DGCResultContext dgcResultContext) {
    final GarbageCollectionInfo gcInfo = dgcResultContext.getGCInfo();
    final SortedSet<ObjectID> sortedGarbage = dgcResultContext.getGarbageIDs();

    this.publisher.fireGCDeleteEvent(gcInfo);
    gcInfo.setActualGarbageCount(sortedGarbage.size());
    final long start = System.currentTimeMillis();

    if (logger.isDebugEnabled()) {
      logger.debug("Deleting objects: " + sortedGarbage);
    }
    this.objectStore.removeAllObjectsByIDNow(sortedGarbage);

    final long elapsed = System.currentTimeMillis() - start;
    gcInfo.setDeleteStageTime(elapsed);
    final long elapsedTime = System.currentTimeMillis() - gcInfo.getStartTime();
    gcInfo.setElapsedTime(elapsedTime);
    gcInfo.setEndObjectCount(this.objectStore.getObjectCount());
    this.publisher.fireGCCompletedEvent(gcInfo);
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.objectStore = scc.getObjectStore();
  }
}
