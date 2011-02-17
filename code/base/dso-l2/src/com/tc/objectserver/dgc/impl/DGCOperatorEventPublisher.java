/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.operatorevent.TerracottaOperatorEventFactory;

public class DGCOperatorEventPublisher extends GarbageCollectorEventListenerAdapter {
  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  @Override
  public void garbageCollectorStart(GarbageCollectionInfo info) {
    this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory
        .createDGCStartedEvent(info.getIteration()));
  }

  @Override
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createDGCFinishedEvent(info
        .getIteration(), info.getBeginObjectCount(), info.getActualGarbageCount(), info.getElapsedTime(), info
        .getEndObjectCount()));
  }

  @Override
  public void garbageCollectorCanceled(GarbageCollectionInfo info) {
    this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createDGCCanceledEvent(info
        .getIteration()));
  }
}
