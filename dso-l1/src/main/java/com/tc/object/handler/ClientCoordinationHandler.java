/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeRefusedMessage;

public class ClientCoordinationHandler extends AbstractEventHandler {

  private static final TCLogger  consoleLogger = CustomerLogging.getConsoleLogger();
  private ClientHandshakeManager handshakeManager;

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ClientHandshakeRefusedMessage) {
      consoleLogger.error(((ClientHandshakeRefusedMessage) context).getRefualsCause());
      consoleLogger.info("L1 Exiting...");
      throw new RuntimeException(((ClientHandshakeRefusedMessage) context).getRefualsCause());
    } else if (context instanceof ClientHandshakeAckMessage) {
      handleClientHandshakeAckMessage((ClientHandshakeAckMessage) context);
    } else if (context instanceof PauseContext) {
      handlePauseContext((PauseContext) context);
    } else {
      throw new AssertionError("unknown event type: " + context.getClass().getName());
    }
  }

  private void handlePauseContext(final PauseContext ctxt) {
    if (ctxt.getIsPause()) {
      handshakeManager.disconnected(ctxt.getRemoteNode());
    } else {
      handshakeManager.connected(ctxt.getRemoteNode());
    }
  }

  private void handleClientHandshakeAckMessage(final ClientHandshakeAckMessage handshakeAck) {
    handshakeManager.acknowledgeHandshake(handshakeAck);
  }

  @Override
  public synchronized void initialize(final ConfigurationContext context) {
    super.initialize(context);
    this.handshakeManager = ((ClientConfigurationContext) context).getClientHandshakeManager();
  }

}
