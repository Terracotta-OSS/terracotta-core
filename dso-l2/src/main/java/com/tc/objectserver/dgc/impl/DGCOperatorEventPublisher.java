/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
    if (info.isInlineDGC()) { return; }
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
    if (info.isInlineDGC()) { return; }
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
    if (info.isInlineDGC()) { return; }
    if (info.isInlineCleanup()) {
      this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory
          .createInlineDGCCleanupCanceledEvent(info.getIteration()));
    } else {
      this.operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createDGCCanceledEvent(info
          .getIteration()));
    }
  }
}
