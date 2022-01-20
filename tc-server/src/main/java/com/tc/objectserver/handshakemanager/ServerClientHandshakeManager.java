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
import com.tc.bytes.TCByteBufferFactory;

import org.slf4j.Logger;

import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.exception.ServerException;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ServerMode;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.utils.L2Utils;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.FetchID;
import com.tc.object.msg.ClientEntityReferenceContext;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.entity.LocalPipelineFlushMessage;
import com.tc.objectserver.entity.PlatformEntity;
import com.tc.objectserver.entity.ReconnectListener;
import com.tc.objectserver.entity.ReferenceMessage;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public class ServerClientHandshakeManager {
  private static enum State {
    INIT,
    STARTING,
    STARTED,
  }
  static final int                       RECONNECT_WARN_INTERVAL           = 15000;
  private static final boolean           SHOULD_SEND_STATS                 = TCPropertiesImpl.getProperties().getBoolean("client.send.stats", false);
  private State                          state                             = State.INIT;
  private final List<ReconnectListener>     waitingForReconnect = new ArrayList<>();

  private final Timer                    timer;
  private final Supplier<Long>           reconnectTimeoutSupplier;
  private final DSOChannelManager        channelManager;
  private final ConsistencyManager       consistency;
  private final Logger logger;
  private final Set<ClientID>            unconnectedClients        = new HashSet<>();
  private final Logger consoleLogger;
  private final Sink<VoltronEntityMessage> voltron;
  private final ProductInfo productInfo;

  public ServerClientHandshakeManager(Logger logger, ConsistencyManager consistency, DSOChannelManager channelManager,
                                      Timer timer, Supplier<Long> reconnectTimeoutSupplier, Sink<VoltronEntityMessage> voltron,
                                      ProductInfo product,
                                      Logger consoleLogger) {
    this.logger = logger;
    this.channelManager = channelManager;
    this.reconnectTimeoutSupplier = reconnectTimeoutSupplier;
    this.timer = timer;
    this.voltron = voltron;
    this.consoleLogger = consoleLogger;
    this.consistency = consistency;
    this.productInfo = product;
  }

  public synchronized boolean isStarting() {
    return this.state == State.STARTING;
  }

  public synchronized boolean isStarted() {
    return this.state == State.STARTED;
  }
  
  private boolean canAcceptStats(String version) {
    return SHOULD_SEND_STATS && version.equals(productInfo.version());
  }

  public void notifyClientConnect(ClientHandshakeMessage handshake, EntityManager entityManager, ProcessTransactionHandler transactionHandler) throws ClientHandshakeException {
    final ClientID clientID = (ClientID) handshake.getSourceNodeID();
    long save = clientID.toLong();
    synchronized (this) {
      this.logger.info("Handling client handshake for " + clientID);
      handshake.getChannel().addAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT, 
          new ClientHandshakeMonitoringInfo(handshake.getClientPID(), handshake.getUUID(), handshake.getName(), handshake.getClientVersion(), handshake.getClientRevision(), handshake.getClientAddress()), false);
      if (canAcceptStats(handshake.getClientVersion())) {
        handshake.getChannel().addAttachment("SendStats", true, true);
      }
      this.logger.info("confirming client handshake for " + state + " " + save + " " + clientID);
      if (this.state == State.STARTED) {
        Assert.assertEquals(save, clientID.toLong());
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
          } catch (ServerException e) {
            // We don't expect to fail at this point.
            // TODO:  Determine if we have a meaningful way to handle this error.
            throw Assert.failure("Unexpected failure to get entity in handshake", e);
          }

          if (entity.isPresent()) {
            byte[] extendedReconnectData = referenceContext.getExtendedReconnectData();
            ReferenceMessage msg = new ReferenceMessage(clientID, true, descriptor, TCByteBufferFactory.wrap(extendedReconnectData));
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
        
        if (connectClient(clientID)) {
          this.consoleLogger.info("Last unconnected client ({}) now connected.  Reconnection starting", clientID);
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
    clientMsg.getChannel().addAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT, 
        new ClientHandshakeMonitoringInfo(clientMsg.getClientPID(), clientMsg.getUUID(), clientMsg.getName(), clientMsg.getClientVersion(), clientMsg.getClientRevision(), clientMsg.getClientAddress()), false);
    ClientHandshakeAckMessage ack = (ClientHandshakeAckMessage)clientMsg.getChannel().createMessage(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE);
    ack.initialize(Collections.emptySet(), clientID, productInfo.version());
    ack.send();
  }  

  private void sendAckMessageFor(ClientID clientID) {
    this.logger.info("Sending handshake acknowledgement to " + clientID);

    // NOTE: handshake ack message initialize()/send() must be done atomically with making the channel active
    // and is thus done inside this channel manager call
    this.channelManager.makeChannelActive(clientID);
  }

  synchronized void notifyTimeout() {
    if (!isStarted()) {
      this.logger
          .info("Reconnect window closing.  Killing any previously connected clients that failed to connect in time: "
                + this.unconnectedClients);
      this.channelManager.closeAll(this.unconnectedClients);
      this.unconnectedClients.clear();
      this.consoleLogger.info("Reconnect window closed. All dead clients removed.");
      start();
    } else {
      this.consoleLogger.info("Reconnect window closed, but server already started.");
    }
  }

  // Should be called from within the sync block
  private void start() {
    this.timer.cancel();
    final Set<NodeID> cids = Collections.unmodifiableSet(this.channelManager.getAllClientIDs());
    if (!cids.isEmpty()) {
      this.consoleLogger.info("Reconnection with {} clients ", cids.size());
      if (cids.size() <= 10) {
        this.consoleLogger.info("Reconnected clients - {}", cids);
      }
    }
    while (!cids.isEmpty() && !this.consistency.requestTransition(ServerMode.ACTIVE, ClientID.NULL_ID, ConsistencyManager.Transition.ADD_CLIENT)) {
      consoleLogger.info("request to add reconnect clients has been rejected, will try again in 5 seconds");
      try {
        TimeUnit.SECONDS.sleep(5);
      } catch (InterruptedException i) {
        L2Utils.handleInterrupted(null, i);
      }
    }
    // It is important to start all the managers before sending the ack to the clients
    for (NodeID nid : cids) {
      final ClientID clientID = (ClientID) nid;
      if (this.channelManager.isActiveID(clientID)) {
        sendAckMessageFor(clientID);
      }
    }
    this.state = State.STARTED;
    notifyComplete(!cids.isEmpty());
    // Tell the transaction handler the message to replay any resends we received.  Schedule a noop 
    // in case all the clients are waiting on resends
    voltron.addToSink(new LocalPipelineFlushMessage(EntityDescriptor.createDescriptorForInvoke(PlatformEntity.PLATFORM_FETCH_ID, ClientInstanceID.NULL_ID), false));
  }

  public void stop() {
    timer.cancel();
    this.state = State.INIT;
  }
  
  private void notifyComplete(boolean log) {
    if (log) {
      consoleLogger.info("Reconnection complete");
    }
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
        this.unconnectedClients.add(connID);
      }
      startReconnectWindow();
    }
  }

  private void startReconnectWindow() {
    long reconnectTimeout = reconnectTimeoutSupplier.get();
    String message = "Starting reconnect window: " + reconnectTimeout + " ms. Waiting for "
                     + getUnconnectedClientsSize() + " clients to connect.";
    if (getUnconnectedClientsSize() <= 10) {
      message += " Unconnected Clients - " + getUnconnectedClients();
    }
    this.consoleLogger.info(message);

    ReconnectTimerTask reconnectTimerTask = new ReconnectTimerTask(reconnectTimeout);
    if (reconnectTimeout < RECONNECT_WARN_INTERVAL) {
      scheduleTask(reconnectTimerTask, reconnectTimeout);
    } else {
      scheduleTask(reconnectTimerTask, RECONNECT_WARN_INTERVAL, RECONNECT_WARN_INTERVAL);
    }
  }

  private void scheduleTask(ReconnectTimerTask task, long delay) {
    try {
      this.timer.schedule(task, delay);
    } catch (IllegalStateException state) {
      logger.info("task not scheduled", state);
    }
  }

  private void scheduleTask(ReconnectTimerTask task, long delay, long period) {
    try {
      this.timer.schedule(task, delay, period);
    } catch (IllegalStateException state) {
      logger.info("task not scheduled", state);
    }
  }

  private void assertInit() {
    if (this.state != State.INIT) { throw new AssertionError("Should be in STARTING state: " + this.state); }
  }

  synchronized Collection<ClientID> getUnconnectedClients() {
    return new ArrayList<>(this.unconnectedClients);
  }

  synchronized int getUnconnectedClientsSize() {
    return this.unconnectedClients.size();
  }

  synchronized boolean connectClient(ClientID cid) {
    consoleLogger.info("Connecting client {}", cid);
    return this.unconnectedClients.remove(cid) && this.unconnectedClients.isEmpty();
  }

  /**
   * Notifies handshake manager that the reconnect time has passed.
   * 
   * @author orion
   */
  private class ReconnectTimerTask extends TimerTask {

    private long                               timeToWait;

    private ReconnectTimerTask(long timeToWait) {
      this.timeToWait = timeToWait;
    }

    @Override
    public boolean cancel() {
      return super.cancel();
    }

    @Override
    public void run() {
      this.timeToWait -= RECONNECT_WARN_INTERVAL;
      if (this.timeToWait > 0 && ServerClientHandshakeManager.this.getUnconnectedClientsSize() > 0) {

        String message = "Reconnect window active.  Waiting for " + ServerClientHandshakeManager.this.getUnconnectedClientsSize()
                         + " clients to connect. " + this.timeToWait + " ms remaining.";
        if (ServerClientHandshakeManager.this.getUnconnectedClientsSize() <= 10) {
          message += " Unconnected Clients - " + getUnconnectedClients();
        }
        ServerClientHandshakeManager.this.consoleLogger.info(message);

        if (this.timeToWait < RECONNECT_WARN_INTERVAL) {
          cancel();
          final ReconnectTimerTask task = new ReconnectTimerTask(this.timeToWait);
          scheduleTask(task, this.timeToWait);
        }
      } else {
        ServerClientHandshakeManager.this.notifyTimeout();
      }
    }
  }
}
