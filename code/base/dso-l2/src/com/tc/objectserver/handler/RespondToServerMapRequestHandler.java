/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.context.EntryForKeyResponseContext;
import com.tc.objectserver.context.ServerMapMissingObjectResponseContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class RespondToServerMapRequestHandler extends AbstractEventHandler implements EventHandler {

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
