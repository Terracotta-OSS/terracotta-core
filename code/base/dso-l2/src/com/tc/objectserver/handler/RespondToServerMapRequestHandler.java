/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.context.EntryForKeyResponseContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class RespondToServerMapRequestHandler extends AbstractEventHandler implements EventHandler {

  private ServerMapRequestManager serverTCMapRequestManager;
 
  @Override
  public void handleEvent(final EventContext context) {
    final EntryForKeyResponseContext responseContext = (EntryForKeyResponseContext) context;

    final ObjectID mapID = responseContext.getMapID();
    final ManagedObject mo = responseContext.getManagedObject();
    
    serverTCMapRequestManager.sendResponseFor(mapID, mo);
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.serverTCMapRequestManager = oscc.getServerTCMapRequestManager();
  }

}
