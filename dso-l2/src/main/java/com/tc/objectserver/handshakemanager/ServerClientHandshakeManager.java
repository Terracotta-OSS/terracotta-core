/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handshakemanager;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.exception.EntityException;

import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.msg.ClientEntityReferenceContext;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.entity.ClientDescriptorImpl;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.locks.LockManager;
import com.tc.util.Assert;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class ServerClientHandshakeManager {
  private static enum State {
    INIT,
    STARTING,
    STARTED,
  }
  static final int                       RECONNECT_WARN_INTERVAL           = 15000;

  private State                          state                             = State.INIT;

  private final Timer                    timer;
  private final ReconnectTimerTask       reconnectTimerTask;
  private final LockManager              lockManager;
  private final EntityManager entityManager;
  private final ProcessTransactionHandler transactionHandler;
  private final long                     reconnectTimeout;
  private final DSOChannelManager        channelManager;
  private final TCLogger                 logger;
  private final Set<ClientID>            existingUnconnectedClients        = new HashSet<>();
  private final boolean                  persistent;
  private final TCLogger                 consoleLogger;

  public ServerClientHandshakeManager(TCLogger logger, DSOChannelManager channelManager,
                                      LockManager lockManager, EntityManager entityManager, ProcessTransactionHandler transactionHandler,
                                      Timer timer, long reconnectTimeout,
                                      boolean persistent, TCLogger consoleLogger) {
    this.logger = logger;
    this.channelManager = channelManager;
    this.lockManager = lockManager;
    this.entityManager = entityManager;
    this.transactionHandler = transactionHandler;
    this.reconnectTimeout = reconnectTimeout;
    this.timer = timer;
    this.persistent = persistent;
    this.consoleLogger = consoleLogger;
    this.reconnectTimerTask = new ReconnectTimerTask(this, timer);
  }

  public synchronized boolean isStarting() {
    return this.state == State.STARTING;
  }

  public synchronized boolean isStarted() {
    return this.state == State.STARTED;
  }

  public void notifyClientConnect(ClientHandshakeMessage handshake) throws ClientHandshakeException {
    final ClientID clientID = (ClientID) handshake.getSourceNodeID();
    synchronized (this) {
      this.logger.info("Handling client handshake for " + clientID);

      Collection<ClientServerExchangeLockContext> lockContexts = handshake.getLockContexts();
      if (this.state == State.STARTED) {
        // This is a normal connection handshake, from a new client connecting once the server is up and running.
        
        for (final ClientServerExchangeLockContext context : lockContexts) {
          if (context.getState() == com.tc.object.locks.ServerLockContext.State.WAITER) {
            throw new ClientHandshakeException("Client " + clientID + " connected after startup should have no existing wait contexts.");
          }
        }
        sendAckMessageFor(clientID);
      } else if (this.state == State.STARTING) {
        // This is a client reconnecting after a restart.
        
        this.channelManager.makeChannelActiveNoAck(handshake.getChannel());

        // NOTE:  Sequence validation temporarily removed since reconnect handshake isn't yet fully implemented for 5.0.
        this.logger.warn("TODO:  validate transaction sequence IDs");
//        this.sequenceValidator.initSequence(clientID, handshake.getTransactionSequenceIDs());

        // TODO: Link up the loaded entities from a client to this

        this.lockManager.reestablishState(clientID, lockContexts);
        
        // Find any client-entity references and ensure that we account for them.
        for(ClientEntityReferenceContext referenceContext : handshake.getReconnectReferences()) {
          EntityID entityID = referenceContext.getEntityID();
          long version = referenceContext.getEntityVersion();
          Optional<ManagedEntity> entity = null;
          try {
            entity = this.entityManager.getEntity(entityID, version);
          } catch (EntityException e) {
            // We don't expect to fail at this point.
            // TODO:  Determine if we have a meaningful way to handle this error.
            Assert.failure("Unexpected failure to get entity in handshake", e);
          }
          // If we fail to find this, something is seriously wrong since either the restart/failover was incorrect or this message is invalid.
          // TODO:  Determine if we have a meaningful way to handle this error.
          Assert.assertTrue(entity.isPresent());
          EntityDescriptor entityDescriptor = referenceContext.getEntityDescriptor();
          ClientDescriptor clientDescriptor = new ClientDescriptorImpl(clientID, entityDescriptor);
          byte[] extendedReconnectData = referenceContext.getExtendedReconnectData();
          entity.get().reconnectClient(clientID, clientDescriptor, extendedReconnectData);
        }
        
        // Find any resent messages and re-apply them in the transaction handler.
        for (ResendVoltronEntityMessage resentMessage : handshake.getResendMessages()) {
          this.transactionHandler.handleResentMessage(resentMessage);
        }

        // Now that we have processed everything from this resend, see if it was the last one.
        this.logger.debug("Removing client " + clientID + " from set of existing unconnected clients.");
        this.existingUnconnectedClients.remove(clientID);
        if (this.existingUnconnectedClients.isEmpty()) {
          this.logger.debug("Last existing unconnected client (" + clientID + ") now connected.  Cancelling timer");
          this.timer.cancel();
          start();
        }
      } else {
        // This is an unexpected state.  We should only be able to receive handshakes while STARTING (reconnect) or STARTED (new clients).
        Assert.fail();
      }
    }
  }

  public void notifyClientRefused(ClientHandshakeMessage clientMsg, String message) {
    final ClientID clientID = (ClientID) clientMsg.getSourceNodeID();
    this.channelManager.makeChannelRefuse(clientID, message);
  }

  private void sendAckMessageFor(ClientID clientID) {
    this.logger.info("Sending handshake acknowledgement to " + clientID);

    // NOTE: handshake ack message initialize()/send() must be done atomically with making the channel active
    // and is thus done inside this channel manager call
    this.channelManager.makeChannelActive(clientID, this.persistent);
  }

  public synchronized void notifyTimeout() {
    if (!isStarted()) {
      this.logger
          .info("Reconnect window closing.  Killing any previously connected clients that failed to connect in time: "
                + this.existingUnconnectedClients);
      this.channelManager.closeAll(this.existingUnconnectedClients);
      this.existingUnconnectedClients.clear();
      this.consoleLogger.info("Reconnect window closed. All dead clients removed.");
      start();
    } else {
      this.consoleLogger.info("Reconnect window closed, but server already started.");
    }
  }

  // Should be called from within the sync block
  private void start() {
    this.logger.info("Starting TSA services...");
    this.lockManager.start();
    final Set<NodeID> cids = Collections.unmodifiableSet(this.channelManager.getAllClientIDs());
    // It is important to start all the managers before sending the ack to the clients
    for (NodeID nid : cids) {
      final ClientID clientID = (ClientID) nid;
      sendAckMessageFor(clientID);
    }
    // Tell the transaction handler the message to replay any resends we received.
    this.transactionHandler.executeAllResends();
    this.state = State.STARTED;
  }

  public synchronized void setStarting(Set<ConnectionID> existingConnections) {
    assertInit();
    this.state = State.STARTING;
    if (existingConnections.isEmpty()) {
      start();
    } else {
      for (ConnectionID connID : existingConnections) {
        this.existingUnconnectedClients.add(this.channelManager.getClientIDFor(new ChannelID(connID.getChannelID())));
      }
    }
  }

  public void startReconnectWindow() {
    String message = "Starting reconnect window: " + this.reconnectTimeout + " ms. Waiting for "
                     + this.existingUnconnectedClients.size() + " clients to connect.";
    if (this.existingUnconnectedClients.size() <= 10) {
      message += " Unconnected Clients - " + this.existingUnconnectedClients;
    }
    this.consoleLogger.info(message);

    if (this.reconnectTimeout < RECONNECT_WARN_INTERVAL) {
      this.timer.schedule(this.reconnectTimerTask, this.reconnectTimeout);
    } else {
      this.timer.schedule(this.reconnectTimerTask, RECONNECT_WARN_INTERVAL, RECONNECT_WARN_INTERVAL);
    }
  }

  private void assertInit() {
    if (this.state != State.INIT) { throw new AssertionError("Should be in STARTING state: " + this.state); }
  }

  synchronized int getUnconnectedClientsSize() {
    return this.existingUnconnectedClients.size();
  }

  synchronized Set<ClientID> getUnconnectedClients() {
    return this.existingUnconnectedClients;
  }

  /**
   * Notifies handshake manager that the reconnect time has passed.
   * 
   * @author orion
   */
  private static class ReconnectTimerTask extends TimerTask {

    private final Timer                        timer;
    private final ServerClientHandshakeManager handshakeManager;
    private long                               timeToWait;

    private ReconnectTimerTask(ServerClientHandshakeManager handshakeManager, Timer timer) {
      this.handshakeManager = handshakeManager;
      this.timer = timer;
      this.timeToWait = handshakeManager.reconnectTimeout;
    }

    public void setTimeToWait(long timeToWait) {
      this.timeToWait = timeToWait;
    }

    @Override
    public void run() {
      this.timeToWait -= RECONNECT_WARN_INTERVAL;
      if (this.timeToWait > 0 && this.handshakeManager.getUnconnectedClientsSize() > 0) {

        String message = "Reconnect window active.  Waiting for " + this.handshakeManager.getUnconnectedClientsSize()
                         + " clients to connect. " + this.timeToWait + " ms remaining.";
        if (this.handshakeManager.getUnconnectedClientsSize() <= 10) {
          message += " Unconnected Clients - " + this.handshakeManager.getUnconnectedClients();
        }
        this.handshakeManager.consoleLogger.info(message);

        if (this.timeToWait < RECONNECT_WARN_INTERVAL) {
          cancel();
          final ReconnectTimerTask task = new ReconnectTimerTask(this.handshakeManager, this.timer);
          task.setTimeToWait(this.timeToWait);
          this.timer.schedule(task, this.timeToWait);
        }
      } else {
        this.timer.cancel();
        this.handshakeManager.notifyTimeout();
      }
    }
  }
}
