/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.ConnectionInfo;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.ConnectionInfoConfig;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.platform.rejoin.RejoinManager;

import java.util.HashSet;
import java.util.Set;

public class ClientShutdownManager {
  private static final TCLogger                    logger         = TCLogging.getLogger(ClientShutdownManager.class);
  private final RemoteTransactionManager           rtxManager;

  private final DSOClientMessageChannel            channel;

  private final ClientHandshakeManager             handshakeManager;
  private final PreparedComponentsFromL2Connection connectionComponents;
  private final Set<Runnable>                      beforeShutdown = new HashSet<Runnable>();
  private final DistributedObjectClient            client;
  private final RejoinManager                      rejoinManager;

  public ClientShutdownManager(ClientObjectManager objectManager, DistributedObjectClient client,
                               PreparedComponentsFromL2Connection connectionComponents, RejoinManager rejoinManager) {
    this.client = client;
    this.rejoinManager = rejoinManager;
    this.rtxManager = client.getRemoteTransactionManager();
    this.channel = client.getChannel();
    this.handshakeManager = client.getClientHandshakeManager();
    this.connectionComponents = connectionComponents;
  }

  public void registerBeforeShutdownHook(Runnable beforeShutdownHook) {
    synchronized (beforeShutdown) {
      beforeShutdown.add(beforeShutdownHook);
    }
  }

  public void unregisterBeforeShutdownHook(Runnable beforeShutdownHook) {
    synchronized (beforeShutdown) {
      beforeShutdown.remove(beforeShutdownHook);
    }
  }

  private void executeBeforeShutdownHooks() {
    Runnable[] beforeShutdowns;
    synchronized (beforeShutdown) {
      beforeShutdowns = beforeShutdown.toArray(new Runnable[beforeShutdown.size()]);
    }
    for (Runnable runnable : beforeShutdowns) {
      runnable.run();
    }
  }

  public void execute(boolean fromShutdownHook, boolean forceImmediate) {
    // no more rejoins should happen after shutdown
    rejoinManager.shutdown();

    executeBeforeShutdownHooks();

    closeLocalWork(fromShutdownHook, forceImmediate);

    if (!fromShutdownHook) {
      shutdown();
    } else {
      // for case of reconnect enabled to send out good bye message at channel close
      if (channel != null) {
        try {
          channel.close();
        } catch (Throwable t) {
          logger.error("Error closing channel", t);
        }
      }
    }
  }

  private void closeLocalWork(boolean fromShutdownHook, boolean forceImmediate) {

    // stop handshaking while shutting down
    handshakeManager.shutdown(fromShutdownHook);

    boolean immediate = forceImmediate || isImmediate();
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

    // If we've connected to a persistent server, we should try to flush
    if (handshakeManager.serverIsPersistent()) { return false; }

    // If we think there is more than one server out there, we should try to flush
    ConnectionInfoConfig connectionInfoItem = this.connectionComponents.createConnectionInfoConfigItem();
    ConnectionInfo[] connectionInfo = connectionInfoItem.getConnectionInfos();
    return connectionInfo.length == 1;
  }

  private void shutdown() {
    client.shutdown();

  }
}
