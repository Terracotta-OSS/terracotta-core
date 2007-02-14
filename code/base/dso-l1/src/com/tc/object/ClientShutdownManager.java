/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.StageManager;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.tx.RemoteTransactionManager;

public class ClientShutdownManager {
  private static final TCLogger                    logger = TCLogging.getLogger(ClientShutdownManager.class);
  private final RemoteTransactionManager           rtxManager;
  private final StageManager                       stageManager;
  private final ClientObjectManager                objectManager;
  private final DSOClientMessageChannel            channel;
  private final CommunicationsManager              commsManager;
  private final ClientHandshakeManager             handshakeManager;
  private final PreparedComponentsFromL2Connection connectionComponents;

  public ClientShutdownManager(ClientObjectManager objectManager, RemoteTransactionManager rtxManager,
                               StageManager stageManager, CommunicationsManager commsManager,
                               DSOClientMessageChannel channel, ClientHandshakeManager handshakeManager,
                               PreparedComponentsFromL2Connection connectionComponents) {
    this.objectManager = objectManager;
    this.rtxManager = rtxManager;
    this.stageManager = stageManager;
    this.commsManager = commsManager;
    this.channel = channel;
    this.handshakeManager = handshakeManager;
    this.connectionComponents = connectionComponents;
  }

  public void execute(boolean fromShutdownHook) {
    closeLocalWork();

    if (!fromShutdownHook) {
      shutdown();
    }
  }

  private void closeLocalWork() {

    boolean immediate = isImmediate();
    if (!immediate) {
      if (rtxManager != null) {
        try {
          rtxManager.stop();
        } catch (Throwable t) {
          logger.error("Error shutting down remote transaction manager", t);
        }
      }
    } else {
      logger.warn("DSO Client exiting without flushing local work");
    }

  }

  private boolean isImmediate() {
    // XXX: Race condition here --> we can start the non-immediate shutdown procedure becuase we think the channel is
    // open, but it can die before we start (or in the middle of) flushing the local work
    if (channel.isConnected()) { return false; }

    // If we've connected to a persisent server, we should try to flush
    if (handshakeManager.serverIsPersistent()) { return false; }

    // If we think there is more than one server out there, we should try to flush
    ConfigItem connectionInfoItem = this.connectionComponents.createConnectionInfoConfigItem();
    ConnectionInfo[] connectionInfo = (ConnectionInfo[]) connectionInfoItem.getObject();
    return connectionInfo.length == 1;
  }

  private void shutdown() {

    if (stageManager != null) {
      try {
        stageManager.stopAll();
      } catch (Throwable t) {
        logger.error("Error stopping stage manager", t);
      }
    }

    if (objectManager != null) {
      try {
        objectManager.shutdown();
      } catch (Throwable t) {
        logger.error("Error shutting down client object manager", t);
      }
    }

    if (channel != null) {
      try {
        channel.close();
      } catch (Throwable t) {
        logger.error("Error closing channel", t);
      }
    }

    if (commsManager != null) {
      try {
        commsManager.shutdown();
      } catch (Throwable t) {
        logger.error("Error shutting down communications manager", t);
      }
    }
  }

}
