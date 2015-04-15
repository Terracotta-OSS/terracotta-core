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
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;


public class TransactionalStagesCoordinatorImpl implements TransactionalStageCoordinator {

  private Sink               lookupSink;
  private Sink               applySink;

  private final StageManager stageManager;

  public TransactionalStagesCoordinatorImpl(StageManager stageManager) {
    this.stageManager = stageManager;
  }

  public void lookUpSinks() {
    this.lookupSink = stageManager.getStage(ServerConfigurationContext.TRANSACTION_LOOKUP_STAGE).getSink();
    this.applySink = stageManager.getStage(ServerConfigurationContext.APPLY_CHANGES_STAGE).getSink();
  }

  @Override
  public void addToApplyStage(EventContext context) {
    applySink.add(context);
  }

  @Override
  public void initiateLookup() {
    lookupSink.addLossy(new LookupEventContext());
  }

}
