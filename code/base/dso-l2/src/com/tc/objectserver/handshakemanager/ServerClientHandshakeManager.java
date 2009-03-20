/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handshakemanager;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ObjectIDBatchRequest;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.util.SequenceValidator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ServerClientHandshakeManager {

  private static final State             INIT                              = new State("INIT");
  private static final State             STARTING                          = new State("STARTING");
  private static final State             STARTED                           = new State("STARTED");
  private static final int               BATCH_SEQUENCE_SIZE               = 10000;
  static final int                       RECONNECT_WARN_INTERVAL           = 15000;

  public static final Sink               NULL_SINK                         = new NullSink();

  private State                          state                             = INIT;

  private final Timer                    timer;
  private final ReconnectTimerTask       reconnectTimerTask;
  private final ClientStateManager       clientStateManager;
  private final LockManager              lockManager;
  private final Sink                     lockResponseSink;
  private final Sink                     oidRequestSink;
  private final long                     reconnectTimeout;
  private final DSOChannelManager        channelManager;
  private final TCLogger                 logger;
  private final SequenceValidator        sequenceValidator;
  private final Set                      existingUnconnectedClients        = new HashSet();
  private final Set                      clientsRequestingObjectIDSequence = new HashSet();
  private final boolean                  persistent;
  private final ServerTransactionManager transactionManager;
  private final TCLogger                 consoleLogger;
  private final TransactionBatchManager  transactionBatchManager;

  public ServerClientHandshakeManager(TCLogger logger, DSOChannelManager channelManager,
                                      ServerTransactionManager transactionManager,
                                      TransactionBatchManager transactionBatchManager,
                                      SequenceValidator sequenceValidator, ClientStateManager clientStateManager,
                                      LockManager lockManager, Sink lockResponseSink, Sink oidRequestSink, Timer timer,
                                      long reconnectTimeout, boolean persistent, TCLogger consoleLogger) {
    this.logger = logger;
    this.channelManager = channelManager;
    this.transactionManager = transactionManager;
    this.transactionBatchManager = transactionBatchManager;
    this.sequenceValidator = sequenceValidator;
    this.clientStateManager = clientStateManager;
    this.lockManager = lockManager;
    this.lockResponseSink = lockResponseSink;
    this.oidRequestSink = oidRequestSink;
    this.reconnectTimeout = reconnectTimeout;
    this.timer = timer;
    this.persistent = persistent;
    this.consoleLogger = consoleLogger;
    this.reconnectTimerTask = new ReconnectTimerTask(this, timer);
  }

  public synchronized boolean isStarting() {
    return this.state == STARTING;
  }

  public synchronized boolean isStarted() {
    return this.state == STARTED;
  }

  public void notifyClientConnect(ClientHandshakeMessage handshake) throws ClientHandshakeException {
    ClientID clientID = (ClientID) handshake.getSourceNodeID();
    this.logger.info("Client connected " + clientID);
    synchronized (this) {
      this.logger.debug("Handling client handshake...");
      this.clientStateManager.startupNode(clientID);
      if (this.state == STARTED) {
        if (handshake.getObjectIDs().size() > 0) {
          //
          throw new ClientHandshakeException(
                                             "Clients connected after startup should have no existing object references.");
        }
        if (handshake.getWaitContexts().size() > 0) {
          //
          throw new ClientHandshakeException("Clients connected after startup should have no existing wait contexts.");
        }
        if (!handshake.getResentTransactionIDs().isEmpty()) {
          //
          throw new ClientHandshakeException("Clients connected after startup should not resend transactions.");
        }
        if (handshake.isObjectIDsRequested()) {
          this.clientsRequestingObjectIDSequence.add(clientID);
        }
        this.transactionBatchManager.notifyServerHighWaterMark(clientID, handshake.getServerHighWaterMark());
        // XXX: It would be better to not have two different code paths that both call sendAckMessageFor(..)
        sendAckMessageFor(clientID);
        return;
      }

      if (this.state == STARTING) {
        this.channelManager.makeChannelActiveNoAck(handshake.getChannel());
        this.transactionManager.setResentTransactionIDs(clientID, handshake.getResentTransactionIDs());
      }

      this.sequenceValidator.initSequence(clientID, handshake.getTransactionSequenceIDs());

      this.clientStateManager.addReferences(clientID, handshake.getObjectIDs());

      for (Iterator i = handshake.getLockContexts().iterator(); i.hasNext();) {
        LockContext ctxt = (LockContext) i.next();
        this.lockManager.reestablishLock(ctxt.getLockID(), ctxt.getNodeID(), ctxt.getThreadID(), ctxt.getLockLevel(),
                                         this.lockResponseSink);
      }

      for (Iterator i = handshake.getWaitContexts().iterator(); i.hasNext();) {
        WaitContext ctxt = (WaitContext) i.next();
        this.lockManager.reestablishWait(ctxt.getLockID(), ctxt.getNodeID(), ctxt.getThreadID(), ctxt.getLockLevel(),
                                         ctxt.getTimerSpec(), this.lockResponseSink);
      }

      for (Iterator i = handshake.getPendingLockContexts().iterator(); i.hasNext();) {
        LockContext ctxt = (LockContext) i.next();
        this.lockManager.requestLock(ctxt.getLockID(), ctxt.getNodeID(), ctxt.getThreadID(), ctxt.getLockLevel(), ctxt
            .getLockType(), this.lockResponseSink);
      }

      for (Iterator i = handshake.getPendingTryLockContexts().iterator(); i.hasNext();) {
        TryLockContext ctxt = (TryLockContext) i.next();
        this.lockManager.tryRequestLock(ctxt.getLockID(), ctxt.getNodeID(), ctxt.getThreadID(), ctxt.getLockLevel(),
                                        ctxt.getLockType(), ctxt.getTimerSpec(), this.lockResponseSink);
      }

      if (handshake.isObjectIDsRequested()) {
        this.clientsRequestingObjectIDSequence.add(clientID);
      }
      this.transactionBatchManager.notifyServerHighWaterMark(clientID, handshake.getServerHighWaterMark());

      if (this.state == STARTING) {
        this.logger.debug("Removing client " + clientID + " from set of existing unconnected clients.");
        this.existingUnconnectedClients.remove(clientID);
        if (this.existingUnconnectedClients.isEmpty()) {
          this.logger.debug("Last existing unconnected client (" + clientID + ") now connected.  Cancelling timer");
          this.timer.cancel();
          start();
        }
      } else {
        sendAckMessageFor(clientID);
      }
    }
  }

  private void sendAckMessageFor(ClientID clientID) {
    this.logger.debug("Sending handshake acknowledgement to " + clientID);

    // NOTE: handshake ack message initialize()/send() must be done atomically with making the channel active
    // and is thus done inside this channel manager call
    this.channelManager.makeChannelActive(clientID, this.persistent);

    if (this.clientsRequestingObjectIDSequence.remove(clientID)) {
      this.oidRequestSink.add(new ObjectIDBatchRequestImpl(clientID, BATCH_SEQUENCE_SIZE));
    }
  }

  public synchronized void notifyTimeout() {
    if (!isStarted()) {
      this.logger
          .info("Reconnect window closing.  Killing any previously connected clients that failed to connect in time: "
                + this.existingUnconnectedClients);
      this.channelManager.closeAll(this.existingUnconnectedClients);
      for (Iterator i = this.existingUnconnectedClients.iterator(); i.hasNext();) {
        ClientID deadClient = (ClientID) i.next();
        this.clientStateManager.shutdownNode(deadClient);
        i.remove();
      }
      this.consoleLogger.info("Reconnect window closed. All dead clients removed.");
      start();
    } else {
      this.consoleLogger.info("Reconnect window closed, but server already started.");
    }
  }

  // Should be called from within the sync block
  private void start() {
    this.logger.info("Starting DSO services...");
    this.lockManager.start();
    Set cids = Collections.unmodifiableSet(this.channelManager.getAllClientIDs());
    this.transactionManager.start(cids);
    for (Iterator i = cids.iterator(); i.hasNext();) {
      ClientID clientID = (ClientID) i.next();
      sendAckMessageFor(clientID);
    }
    this.state = STARTED;
  }

  public synchronized void setStarting(Set existingConnections) {
    assertInit();
    this.state = STARTING;
    if (existingConnections.isEmpty()) {
      start();
    } else {
      for (Iterator i = existingConnections.iterator(); i.hasNext();) {
        this.existingUnconnectedClients.add(this.channelManager.getClientIDFor(new ChannelID(((ConnectionID) i.next())
            .getChannelID())));
      }

      this.consoleLogger.info("Starting reconnect window: " + this.reconnectTimeout + " ms. Waiting for "
                              + this.existingUnconnectedClients.size() + " clients to connect. ");
      if (this.reconnectTimeout < RECONNECT_WARN_INTERVAL) {
        this.timer.schedule(this.reconnectTimerTask, this.reconnectTimeout);
      } else {
        this.timer.schedule(this.reconnectTimerTask, RECONNECT_WARN_INTERVAL, RECONNECT_WARN_INTERVAL);
      }
    }
  }

  private void assertInit() {
    if (this.state != INIT) { throw new AssertionError("Should be in STARTING state: " + this.state); }
  }

  synchronized int getUnconnectedClientsSize() {
    return this.existingUnconnectedClients.size();
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
        this.handshakeManager.consoleLogger.info("Reconnect window active.  Waiting for "
                                                 + this.handshakeManager.getUnconnectedClientsSize()
                                                 + " clients to connect. " + this.timeToWait + " ms remaining.");
        if (this.timeToWait < RECONNECT_WARN_INTERVAL) {
          cancel();
          ReconnectTimerTask task = new ReconnectTimerTask(this.handshakeManager, this.timer);
          task.setTimeToWait(this.timeToWait);
          this.timer.schedule(task, this.timeToWait);
        }
      } else {
        this.timer.cancel();
        this.handshakeManager.notifyTimeout();
      }
    }
  }

  private static class State {
    private final String name;

    private State(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return getClass().getName() + "[" + this.name + "]";
    }
  }

  private static class ObjectIDBatchRequestImpl implements ObjectIDBatchRequest, EventContext {

    private final NodeID clientID;
    private final int    batchSize;

    public ObjectIDBatchRequestImpl(NodeID clientID, int batchSize) {
      this.clientID = clientID;
      this.batchSize = batchSize;
    }

    public int getBatchSize() {
      return this.batchSize;
    }

    public NodeID getRequestingNodeID() {
      return this.clientID;
    }

  }

}
