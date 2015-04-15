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
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class L2StateChangeHandler extends AbstractEventHandler {

  private StateManager stateManager;

  @Override
  public void handleEvent(EventContext context) {
    StateChangedEvent sce = (StateChangedEvent) context;
    stateManager.fireStateChangedEvent(sce);
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.stateManager = oscc.getL2Coordinator().getStateManager();
  }

}
