/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeRefusedMessage;

public class ClientCoordinationHandler<EC> extends AbstractEventHandler<EC> {

  private static final TCLogger  consoleLogger = CustomerLogging.getConsoleLogger();
  private ClientHandshakeManager clientHandshakeManager;

  @Override
  public void handleEvent(EC context) {
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

  private void handlePauseContext(PauseContext ctxt) {
    if (ctxt.getIsPause()) {
      clientHandshakeManager.disconnected();
    } else {
      clientHandshakeManager.connected();
    }
  }

  private void handleClientHandshakeAckMessage(ClientHandshakeAckMessage handshakeAck) {
    clientHandshakeManager.acknowledgeHandshake(handshakeAck);
  }

  @Override
  public synchronized void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccContext = (ClientConfigurationContext) context;
    this.clientHandshakeManager = ccContext.getClientHandshakeManager();
  }

}
