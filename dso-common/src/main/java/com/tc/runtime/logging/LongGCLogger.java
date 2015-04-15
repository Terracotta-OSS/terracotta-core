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
package com.tc.runtime.logging;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;

public class LongGCLogger implements MemoryEventsListener {

  private static final TCLogger logger = TCLogging.getLogger(LongGCLogger.class);
  private final long            gcTimeout;
  private MemoryUsage           lastMemoryUsage;

  public LongGCLogger(final long gcTimeOut) {
    this.gcTimeout = gcTimeOut;
  }

  @Override
  public void memoryUsed(MemoryUsage currentUsage) {
    if (lastMemoryUsage == null) {
      lastMemoryUsage = currentUsage;
      return;
    }
    long countDiff = currentUsage.getCollectionCount() - lastMemoryUsage.getCollectionCount();
    long timeDiff = currentUsage.getCollectionTime() - lastMemoryUsage.getCollectionTime();
    if (countDiff > 0 && timeDiff > gcTimeout) {

      TerracottaOperatorEvent tcEvent = TerracottaOperatorEventFactory.createLongGCOperatorEvent(
          new Object[] { gcTimeout, countDiff, timeDiff });

      fireLongGCEvent(tcEvent);
    }
    lastMemoryUsage = currentUsage;
  }

  protected void fireLongGCEvent(TerracottaOperatorEvent tcEvent) {
    logger.warn(tcEvent.getEventMessage());
  }
}
