/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.SequenceValidator;
import com.tc.util.TCTimer;
import com.tc.util.sequence.ObjectIDSequence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

public class ServerClientHandshakeManager {

  private static final State             INIT                              = new State("INIT");
  private static final State             STARTING                          = new State("STARTING");
  private static final State             STARTED                           = new State("STARTED");
  private static final int               BATCH_SEQUENCE_SIZE               = 10000;

  public static final Sink               NULL_SINK                         = new NullSink();

  private State                          state                             = INIT;

  private final TCTimer                  timer;
  private final ReconnectTimerTask       reconnectTimerTask;
  private final ClientStateManager       clientStateManager;
  private final LockManager              lockManager;
  private final Sink                     lockResponseSink;
  private final long                     reconnectTimeout;
  private final ObjectRequestManager     objectRequestManager;
  private final DSOChannelManager        channelManager;
  private final TCLogger                 logger;
  private final SequenceValidator        sequenceValidator;
  private final Set                      existingUnconnectedClients        = new HashSet();
  private final ObjectIDSequence         oidSequence;
  private final Set                      clientsRequestingObjectIDSequence = new HashSet();
  private final boolean                  persistent;
  private final ServerTransactionManager transactionManager;

  public ServerClientHandshakeManager(TCLogger logger, DSOChannelManager channelManager,
                                      ObjectRequestManager objectRequestManager,
                                      ServerTransactionManager transactionManager, SequenceValidator sequenceValidator,
                                      ClientStateManager clientStateManager, LockManager lockManager,
                                      Sink lockResponseSink, ObjectIDSequence oidSequence, TCTimer timer,
                                      long reconnectTimeout, boolean persistent) {
    this.logger = logger;
    this.channelManager = channelManager;
    this.objectRequestManager = objectRequestManager;
    this.transactionManager = transactionManager;
    this.sequenceValidator = sequenceValidator;
    this.clientStateManager = clientStateManager;
    this.lockManager = lockManager;
    this.lockResponseSink = lockResponseSink;
    this.oidSequence = oidSequence;
    this.reconnectTimeout = reconnectTimeout;
    this.timer = timer;
    this.persistent = persistent;
    this.reconnectTimerTask = new ReconnectTimerTask(this, timer);
  }

  public synchronized boolean isStarting() {
    return state == STARTING;
  }

  public synchronized boolean isStarted() {
    return state == STARTED;
  }

  public void notifyClientConnect(ClientHandshakeMessage handshake) throws ClientHandshakeException {
    ChannelID channelID = handshake.getChannelID();
    logger.info("Client connected " + channelID);
    synchronized (this) {
      logger.debug("Handling client handshake...");
      if (state == STARTED) {
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
          logger.debug("Client " + channelID + " requested Object ID Sequences ");
          clientsRequestingObjectIDSequence.add(channelID);
        }
        // XXX: It would be better to not have two different code paths that both call sendAckMessageFor(..)
        sendAckMessageFor(channelID);
        return;
      }

      if (state == STARTING) {
        channelManager.makeChannelActiveNoAck(handshake.getChannel());
        transactionManager.setResentTransactionIDs(channelID, handshake.getResentTransactionIDs());
      }

      this.sequenceValidator.initSequence(handshake.getChannelID(), handshake.getTransactionSequenceIDs());

      clientStateManager.addReferences(channelID, handshake.getObjectIDs());

      for (Iterator i = handshake.getLockContexts().iterator(); i.hasNext();) {
        LockContext ctxt = (LockContext) i.next();
        lockManager.reestablishLock(ctxt.getLockID(), ctxt.getChannelID(), ctxt.getThreadID(), ctxt.getLockLevel(),
                                    lockResponseSink);
      }

      for (Iterator i = handshake.getWaitContexts().iterator(); i.hasNext();) {
        WaitContext ctxt = (WaitContext) i.next();
        lockManager.reestablishWait(ctxt.getLockID(), ctxt.getChannelID(), ctxt.getThreadID(), ctxt.getLockLevel(),
                                    ctxt.getWaitInvocation(), lockResponseSink);
      }

      for (Iterator i = handshake.getPendingLockContexts().iterator(); i.hasNext();) {
        LockContext ctxt = (LockContext) i.next();
        if (ctxt.noBlock()) {
          lockManager.tryRequestLock(ctxt.getLockID(), ctxt.getChannelID(), ctxt.getThreadID(), ctxt.getLockLevel(),
                                     lockResponseSink);
        } else {
          lockManager.requestLock(ctxt.getLockID(), ctxt.getChannelID(), ctxt.getThreadID(), ctxt.getLockLevel(),
                                  lockResponseSink);
        }
      }

      if (handshake.isObjectIDsRequested()) {
        logger.debug("Client " + channelID + " requested Object ID Sequences ");
        clientsRequestingObjectIDSequence.add(channelID);
      }

      if (state == STARTING) {
        logger.debug("Removing client " + channelID + " from set of existing unconnected clients.");
        existingUnconnectedClients.remove(channelID);
        if (existingUnconnectedClients.isEmpty()) {
          logger.debug("Last existing unconnected client (" + channelID + ") now connected.  Cancelling timer");
          timer.cancel();
          start();
        }
      } else {
        sendAckMessageFor(channelID);
      }
    }
  }

  private void sendAckMessageFor(ChannelID channelID) {
    logger.debug("Sending handshake acknowledgement to " + channelID);

    final long startIDs;
    final long endIDs;
    if (clientsRequestingObjectIDSequence.remove(channelID)) {
      final long ids = oidSequence.nextObjectIDBatch(BATCH_SEQUENCE_SIZE);
      logger.debug("Giving out Object ID Sequences to " + channelID + " from " + ids + " to "
                   + (ids + BATCH_SEQUENCE_SIZE));

      startIDs = ids;
      endIDs = ids + BATCH_SEQUENCE_SIZE;
    } else {
      startIDs = endIDs = 0;
    }

    // NOTE: handshake ack message initialize()/send() must be done atomically with making the channel active
    // and is thus done inside this channel manager call
    channelManager.makeChannelActive(channelID, startIDs, endIDs, persistent);
  }

  public synchronized void notifyTimeout() {
    assertNotStarted();
    logger.info("Reconnect window closing.  Killing any previously connected clients that failed to connect in time: "
                + existingUnconnectedClients);
    this.channelManager.closeAll(existingUnconnectedClients);
    for (Iterator i = existingUnconnectedClients.iterator(); i.hasNext();) {
      ChannelID deadClient = (ChannelID) i.next();
      this.clientStateManager.shutdownClient(deadClient);
      i.remove();
    }
    logger.info("Reconnect window closed. All dead clients removed.");
    start();
  }

  // Should be called from within the sync block
  private void start() {
    logger.info("Starting DSO services...");
    lockManager.start();
    objectRequestManager.start();
    Set cids = Collections.unmodifiableSet(channelManager.getRawChannelIDs());
    transactionManager.start(cids);
    for (Iterator i = cids.iterator(); i.hasNext();) {
      ChannelID channelID = (ChannelID) i.next();
      sendAckMessageFor(channelID);
    }
    state = STARTED;
  }

  public synchronized void setStarting(Set existingConnections) {
    assertInit();
    state = STARTING;
    if (existingConnections.isEmpty()) {
      start();
    } else {
      for (Iterator i = existingConnections.iterator(); i.hasNext();) {
        existingUnconnectedClients.add(new ChannelID(((ConnectionID) i.next()).getChannelID()));
      }
      logger.info("Starting reconnect window: " + this.reconnectTimeout + " ms.");
      timer.schedule(reconnectTimerTask, this.reconnectTimeout);
    }
  }

  private void assertInit() {
    if (state != INIT) throw new AssertionError("Should be in STARTING state: " + state);
  }

  private void assertNotStarted() {
    if (state == STARTED) throw new AssertionError("In STARTING state, but shouldn't be.");
  }

  /**
   * Notifies handshake manager that the reconnect time has passed.
   * 
   * @author orion
   */
  private static class ReconnectTimerTask extends TimerTask {

    private final TCTimer                      timer;
    private final ServerClientHandshakeManager handshakeManager;

    private ReconnectTimerTask(ServerClientHandshakeManager handshakeManager, TCTimer timer) {
      this.handshakeManager = handshakeManager;
      this.timer = timer;
    }

    public void run() {
      timer.cancel();
      handshakeManager.notifyTimeout();
    }

  }

  private static class State {
    private final String name;

    private State(String name) {
      this.name = name;
    }

    public String toString() {
      return getClass().getName() + "[" + name + "]";
    }
  }

}
