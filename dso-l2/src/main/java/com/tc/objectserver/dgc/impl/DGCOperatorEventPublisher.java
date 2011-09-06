/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;

public class DGCOperatorEventPublisher extends GarbageCollectorEventListenerAdapter {
  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  @Override
  public void garbageCollectorStart(GarbageCollectionInfo info) {
    if (info.isInlineCleanup()) {
      this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createInlineDGCCleanupStartedEvent(info
          .getIteration()));
    } else {
      this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createDGCStartedEvent(info
          .getIteration()));
    }
  }

  @Override
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    if (info.isInlineCleanup()) {
      this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory
          .createInlineDGCCleanupFinishedEvent(info.getIteration(), info.getBeginObjectCount(),
                                               info.getActualGarbageCount(), info.getElapsedTime(),
                                               info.getEndObjectCount()));
    } else {
      this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createDGCFinishedEvent(info
          .getIteration(), info.getBeginObjectCount(), info.getActualGarbageCount(), info.getElapsedTime(), info
          .getEndObjectCount()));
    }
  }

  @Override
  public void garbageCollectorCanceled(GarbageCollectionInfo info) {
    if (info.isInlineCleanup()) {
      this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory
          .createInlineDGCCleanupCanceledEvent(info.getIteration()));
    } else {
      this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createDGCCanceledEvent(info
          .getIteration()));
    }
  }
}
