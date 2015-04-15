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
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.context.LocksToRecallContext;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;

import java.util.Set;

public class LockRecallHandler extends AbstractEventHandler implements LockRecaller {

  private ClientLockManager lockManager;

  @Override
  public void handleEvent(final EventContext context) {
    final LocksToRecallContext recallContext = (LocksToRecallContext) context;
    recallLocksInline(recallContext.getLocksToRecall());
  }

  @Override
  public void recallLocksInline(final Set<LockID> locks) {
    for (final LockID lock : locks) {
      // batching can cause race explained in ENG-422
      this.lockManager.recall(null, null, lock, ServerLockLevel.WRITE, -1, false);
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.lockManager = ccc.getLockManager();
  }

}
