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
import com.tc.object.context.RejoinContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeRefusedMessage;
import com.tc.platform.rejoin.RejoinManagerInternal;

public class ClientCoordinationHandler extends AbstractEventHandler {

  private static final TCLogger  consoleLogger = CustomerLogging.getConsoleLogger();
  private ClientHandshakeManager handshakeManager;
  private RejoinManagerInternal  rejoinManager;

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
    } else if (context instanceof RejoinContext) {
      handleRejoinContext((RejoinContext) context);
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
  
  private void handleRejoinContext(final RejoinContext context) {
    rejoinManager.initiateRejoin(context.getMessageChannel());
  }

  private void handleClientHandshakeAckMessage(final ClientHandshakeAckMessage handshakeAck) {
    handshakeManager.acknowledgeHandshake(handshakeAck);
  }

  @Override
  public synchronized void initialize(final ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccContext = (ClientConfigurationContext) context;
    this.handshakeManager = ccContext.getClientHandshakeManager();
    this.rejoinManager = ccContext.getRejoinManager();
  }

}
