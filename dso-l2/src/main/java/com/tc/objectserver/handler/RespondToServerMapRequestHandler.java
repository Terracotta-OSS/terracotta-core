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
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.context.EntryForKeyResponseContext;
import com.tc.objectserver.context.ServerMapMissingObjectResponseContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class RespondToServerMapRequestHandler extends AbstractEventHandler {

  private ServerMapRequestManager serverMapRequestManager;

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ServerMapMissingObjectResponseContext) {
      final ServerMapMissingObjectResponseContext responseContext = (ServerMapMissingObjectResponseContext) context;
      serverMapRequestManager.sendMissingObjectResponseFor(responseContext.getMapID());
    } else if (context instanceof EntryForKeyResponseContext) {
      final EntryForKeyResponseContext responseContext = (EntryForKeyResponseContext) context;
      serverMapRequestManager.sendResponseFor(responseContext.getMapID(), responseContext.getManagedObject());
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.serverMapRequestManager = oscc.getServerMapRequestManager();
  }

}
