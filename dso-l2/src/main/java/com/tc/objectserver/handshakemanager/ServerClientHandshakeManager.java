/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.handshakemanager;

import com.tc.async.api.Sink;

import org.slf4j.Logger;
import org.terracotta.exception.EntityException;

import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityDescriptor;
import com.tc.object.msg.ClientEntityReferenceContext;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.entity.LocalPipelineFlushMessage;
import com.tc.objectserver.entity.ReconnectListener;
import com.tc.objectserver.entity.ReferenceMessage;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
  private final List<ReconnectListener>     waitingForReconnect = new ArrayList<>();

  private final Timer                    timer;
  private final ReconnectTimerTask       reconnectTimerTask;
  private final long                     reconnectTimeout;
  private final DSOChannelManager        channelManager;
  private final Logger logger;
  private final Set<ClientID>            existingUnconnectedClients        = new HashSet<>();
  private final Logger consoleLogger;
  private final Sink<VoltronEntityMessage> voltron;

  public ServerClientHandshakeManager(Logger logger, DSOChannelManager channelManager,
                                      Timer timer, long reconnectTimeout,Sink<VoltronEntityMessage> voltron,
                                      Logger consoleLogger) {
    this.logger = logger;
    this.channelManager = channelManager;
    this.reconnectTimeout = reconnectTimeout;
    this.timer = timer;
    this.voltron = voltron;
    this.consoleLogger = consoleLogger;
    this.reconnectTimerTask = new ReconnectTimerTask(this, timer);
  }

  public synchronized boolean isStarting() {
    return this.state == State.STARTING;
  }

  public synchronized boolean isStarted() {
    return this.state == State.STARTED;
  }

  public void notifyClientConnect(ClientHandshakeMessage handshake, EntityManager entityManager, ProcessTransactionHandler transactionHandler) throws ClientHandshakeException {
    final ClientID clientID = (ClientID) handshake.getSourceNodeID();
    synchronized (this) {
      this.logger.info("Handling client handshake for " + clientID);
      handshake.getChannel().addAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT, 
          new ClientHandshakeMonitoringInfo(handshake.getClientPID(), handshake.getUUID(), handshake.getName()), false);

      if (this.state == State.STARTED) {
        // This is a normal connection handshake, from a new client connecting once the server is up and running.
        sendAckMessageFor(clientID);
      } else if (this.state == State.STARTING) {
        // This is a client reconnecting after a restart.
        
        this.channelManager.makeChannelActiveNoAck(handshake.getChannel());
        
        // Find any client-entity references and ensure that we account for them.
        for(ClientEntityReferenceContext referenceContext : handshake.getReconnectReferences()) {
          Optional<ManagedEntity> entity = null;
          EntityDescriptor descriptor = EntityDescriptor.createDescriptorForFetch(referenceContext.getEntityID(), referenceContext.getEntityVersion(), referenceContext.getClientInstanceID());
          try {
            entity = entityManager.getEntity(descriptor);
          } catch (EntityException e) {
            // We don't expect to fail at this point.
            // TODO:  Determine if we have a meaningful way to handle this error.
            throw Assert.failure("Unexpected failure to get entity in handshake", e);
          }

          if (entity.isPresent()) {
            byte[] extendedReconnectData = referenceContext.getExtendedReconnectData();
            ReferenceMessage msg = new ReferenceMessage(clientID, true, descriptor, extendedReconnectData);
            transactionHandler.handleResentReferenceMessage(msg);
          } else {
            throw Assert.failure("entity not found");
          }
        }
        
        // Find any resent messages and re-apply them in the transaction handler.
        for (ResendVoltronEntityMessage resentMessage : handshake.getResendMessages()) {
          logger.debug("RESENT:" + resentMessage.getVoltronType() + " " + resentMessage.getEntityDescriptor());
          transactionHandler.handleResentMessage(resentMessage);
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
  
  public void notifyDiagnosticClient(ClientHandshakeMessage clientMsg) {
    final ClientID clientID = (ClientID) clientMsg.getSourceNodeID();
    ClientHandshakeAckMessage ack = (ClientHandshakeAckMessage)clientMsg.getChannel().createMessage(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE);
    ack.initialize(Collections.emptySet(), clientID, ProductInfo.getInstance().version());
    ack.send();
  }  

  private void sendAckMessageFor(ClientID clientID) {
    this.logger.info("Sending handshake acknowledgement to " + clientID);

    // NOTE: handshake ack message initialize()/send() must be done atomically with making the channel active
    // and is thus done inside this channel manager call
    this.channelManager.makeChannelActive(clientID);
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
    final Set<NodeID> cids = Collections.unmodifiableSet(this.channelManager.getAllClientIDs());
    // It is important to start all the managers before sending the ack to the clients
    for (NodeID nid : cids) {
      final ClientID clientID = (ClientID) nid;
      if (this.channelManager.isActiveID(clientID)) {
        sendAckMessageFor(clientID);
      }
    }
    this.state = State.STARTED;
    notifyComplete();
    // Tell the transaction handler the message to replay any resends we received.  Schedule a noop 
    // in case all the clients are waiting on resends
    voltron.addToSink(new LocalPipelineFlushMessage(EntityDescriptor.NULL_ID, false));
  }
  
  public void notifyComplete() {
    waitingForReconnect.forEach(ReconnectListener::reconnectComplete);
  }
  
  public void addReconnectListener(ReconnectListener rl) {
    waitingForReconnect.add(rl);
  }

  public synchronized void setStarting(Set<ClientID> existingClients) {
    assertInit();
    this.state = State.STARTING;
    if (existingClients.isEmpty()) {
      start();
    } else {
      for (ClientID connID : existingClients) {
        this.existingUnconnectedClients.add(connID);
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
