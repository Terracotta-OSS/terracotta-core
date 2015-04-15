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

import com.tc.async.api.EventContext;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.util.Util;

import java.util.concurrent.CountDownLatch;

public class GarbageCollectContext implements EventContext {

  private final CountDownLatch completionLatch = new CountDownLatch(1);
  private final GCType         type;
  private long                 delay;

  public GarbageCollectContext(final GCType type, final long delay) {
    this.type = type;
    this.delay = delay;
  }

  public GarbageCollectContext(final GCType type) {
    this(type, 0);
  }

  public void done() {
    completionLatch.countDown();
  }

  public void waitForCompletion() {
    boolean isInterrupted = false;
    while (completionLatch.getCount() > 0) {
      try {
        completionLatch.await();
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public GCType getType() {
    return type;
  }

  public long getDelay() {
    return delay;
  }

  public void setDelay(final long delay) {
    this.delay = delay;
  }
}
