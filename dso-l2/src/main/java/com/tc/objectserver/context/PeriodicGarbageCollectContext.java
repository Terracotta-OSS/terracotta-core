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
package com.tc.objectserver.context;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;

public class PeriodicGarbageCollectContext extends GarbageCollectContext {
  private static final TCLogger logger = TCLogging.getLogger(PeriodicGarbageCollectContext.class);
  private final long            interval;

  public PeriodicGarbageCollectContext(final GCType type, final long interval) {
    super(type, interval);
    this.interval = interval;
  }

  @Override
  public void done() {
    // do nothing
  }

  @Override
  public void waitForCompletion() {
    logger.warn("Attempted to wait for completion on Periodic garbage collection.");
  }

  public long getInterval() {
    return interval;
  }

  public void reset() {
    setDelay(interval);
  }
}
