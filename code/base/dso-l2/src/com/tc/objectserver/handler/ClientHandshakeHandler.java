/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;

public class ClientHandshakeHandler extends AbstractEventHandler {

  private ServerClientHandshakeManager handshakeManager;

  public void handleEvent(EventContext context) {
    this.handshakeManager.notifyClientConnect((ClientHandshakeMessage) context);
  }

  public void initialize(ConfigurationContext ctxt) {
    super.initialize(ctxt);
    this.handshakeManager = ((ServerConfigurationContext) ctxt).getClientHandshakeManager();
  }

}
