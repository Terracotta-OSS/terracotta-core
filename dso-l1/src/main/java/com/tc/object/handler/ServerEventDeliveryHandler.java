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
import com.tc.async.api.EventContext;
import com.tc.object.ServerEventListenerManager;
import com.tc.object.context.ServerEventDeliveryContext;

/**
 * Process server events one by one.
 *
 * @author Eugene Shelestovich
 */
public class ServerEventDeliveryHandler extends AbstractEventHandler {

  private final ServerEventListenerManager manager;

  public ServerEventDeliveryHandler(final ServerEventListenerManager manager) { this.manager = manager; }

  @Override
  public void handleEvent(final EventContext ctx) {
    if (ctx instanceof ServerEventDeliveryContext) {
      final ServerEventDeliveryContext msg = (ServerEventDeliveryContext) ctx;

      manager.dispatch(msg.getEvent(), msg.getRemoteNode());
    } else {
      throw new AssertionError("Unknown event type: " + ctx.getClass().getName());
    }
  }
}
