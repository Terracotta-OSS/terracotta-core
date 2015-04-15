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
package com.tc.object.context;

import com.tc.async.api.EventContext;
import com.tc.object.locks.LockID;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class LocksToRecallContext implements EventContext {

  private final Set<LockID>    toRecall;
  private final CountDownLatch latch;

  public LocksToRecallContext(final Set<LockID> toRecall) {
    this.toRecall = toRecall;
    this.latch = new CountDownLatch(1);
  }

  public Set<LockID> getLocksToRecall() {
    return this.toRecall;
  }

  public void waitUntilRecallComplete() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void recallComplete() {
    latch.countDown();
  }

}
