/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.msg.ClientHandshakeAckMessage;

public class PauseHandler extends AbstractEventHandler {

  private ClientHandshakeManager handshakeManager;

  public void handleEvent(EventContext context) {
    if (context instanceof ClientHandshakeAckMessage) {
      ClientHandshakeAckMessage handshakeAck = (ClientHandshakeAckMessage) context;
      handshakeManager.acknowledgeHandshake(handshakeAck.getObjectIDSequenceStart(), handshakeAck
          .getObjectIDSequenceEnd(), handshakeAck.getPersistentServer());
    } else {
      PauseContext ctxt = (PauseContext) context;
      if (ctxt.getIsPause()) {
        handshakeManager.pause();
      } else {
        handshakeManager.unpause();
      }
    }
  }

  public synchronized void initialize(ConfigurationContext context) {
    super.initialize(context);
    this.handshakeManager = ((ClientConfigurationContext) context).getClientHandshakeManager();
  }

}
