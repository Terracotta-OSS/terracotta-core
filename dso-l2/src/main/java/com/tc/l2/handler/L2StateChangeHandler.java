/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.impl.StageController;
import com.tc.l2.context.StateChangedEvent;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.server.TCServerMain;
import com.tc.util.State;
import org.terracotta.server.ServerEnv;


public class L2StateChangeHandler extends AbstractEventHandler<StateChangedEvent> {

  private final StageController stageManager;
  private final ITopologyEventCollector eventCollector;

  public L2StateChangeHandler(StageController stageManager, ITopologyEventCollector eventCollector) {
    this.stageManager = stageManager;
    this.eventCollector = eventCollector;
  }

  @Override
  public void handleEvent(StateChangedEvent sce) {
// execute state transition before notifying any listeners.  Listener notification 
// can happen in any order.  State transition happens in specific order as dictated 
// by the stage controller.
    State newState = sce.getCurrentState();
    // notify the collector that the server's state first to mark the start of transition
    if (sce.movedToActive()) {
//  if this server just became active
      eventCollector.serverDidEnterState(newState, ServerEnv.getServer().getActivateTime());
    } else {
      eventCollector.serverDidEnterState(newState, System.currentTimeMillis());      
    }
    stageManager.transition(sce.getOldState(), newState);
  }
}
