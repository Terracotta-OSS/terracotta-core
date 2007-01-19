/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.SequenceValidator;
import com.tc.util.TCTimer;
import com.tc.util.sequence.ObjectIDSequence;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

public class ServerClientHandshakeManager {

  private static final State              INIT                              = new State("INIT");
  private static final State              STARTING                          = new State("STARTING");
  private static final State              STARTED                           = new State("STARTED");
  private static final int                BATCH_SEQUENCE_SIZE               = 10000;

  public static final Sink                NULL_SINK                         = new NullSink();

  private State                           state                             = INIT;

  private final TCTimer                   timer;
  private final ReconnectTimerTask        reconnectTimerTask;
  private final ClientStateManager        clientStateManager;
  private final LockManager               lockManager;
  private final Sink                      lockResponseSink;
  private final long                      reconnectTimeout;
  private final ObjectManager             objectManager;
  private final Set                       existingUnconnectedClients;
  private final DSOChannelManager         channelManager;
  private final TCLogger                  logger;
  private final SequenceValidator         sequenceValidator;
  private final ServerTransactionManager  transactionManager;
  private final ObjectIDSequence          oidSequence;
  private final Set                       clientsRequestingObjectIDSequence = new HashSet();
  private final boolean                   persistent;
  private final ClientHandshakeAckMessage nullAckMessage                    = new NullClientHandshakeAckMessage();

  public ServerClientHandshakeManager(TCLogger logger, DSOChannelManager channelManager, ObjectManager objectManager,
                                      SequenceValidator sequenceValidator, ClientStateManager clientStateManager,
                                      Set existingUnconnectedClients, LockManager lockManager,
                                      ServerTransactionManager transactionManager, Sink lockResponseSink,
                                      ObjectIDSequence oidSequence, TCTimer timer, long reconnectTimeout,
                                      boolean persistent) {
    this.logger = logger;
    this.channelManager = channelManager;
    this.objectManager = objectManager;
    this.sequenceValidator = sequenceValidator;
    this.clientStateManager = clientStateManager;
    this.lockManager = lockManager;
    this.transactionManager = transactionManager;
    this.lockResponseSink = lockResponseSink;
    this.oidSequence = oidSequence;
    this.reconnectTimeout = reconnectTimeout;
    this.timer = timer;
    this.persistent = persistent;
    this.reconnectTimerTask = new ReconnectTimerTask(this, timer);
    this.existingUnconnectedClients = existingUnconnectedClients;
    if (existingUnconnectedClients.isEmpty()) start();
  }

  public synchronized boolean isStarting() {
    return state == STARTING;
  }

  public synchronized boolean isStarted() {
    return state == STARTED;
  }

  public void notifyClientConnect(ClientHandshakeMessage handshake) {
    ChannelID channelID = handshake.getChannelID();
    logger.info("Client connected " + channelID);
    synchronized (this) {
      logger.debug("Handling client handshake...");
      if (state == INIT) {
        setStarting();
      } else if (state == STARTED) {
        if (handshake.getObjectIDs().size() > 0) {
          //
          throw new AssertionError("Clients connected after startup should have no existing object references.");
        }
        if (handshake.getWaitContexts().size() > 0) {
          //
          throw new AssertionError("Clients connected after startup should have no existing wait contexts.");
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
        channelManager.makeChannelActive(handshake.getChannel(), nullAckMessage);
      }

      this.sequenceValidator.initSequence(handshake.getChannelID(), handshake.getTransactionSequenceIDs());
      this.transactionManager.setResentTransactionIDs(handshake.getChannelID(), handshake.getResentTransactionIDs());

      for (Iterator i = handshake.getObjectIDs().iterator(); i.hasNext();) {
        clientStateManager.addReference(channelID, (ObjectID) i.next());
      }

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
        lockManager.requestLock(ctxt.getLockID(), ctxt.getChannelID(), ctxt.getThreadID(), ctxt.getLockLevel(),
                                lockResponseSink);
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
    try {
      logger.debug("Sending handshake acknowledgement to " + channelID);
      ClientHandshakeAckMessage handshakeAck = channelManager.newClientHandshakeAckMessage(channelID);
      if (clientsRequestingObjectIDSequence.remove(channelID)) {
        long ids = oidSequence.nextObjectIDBatch(BATCH_SEQUENCE_SIZE);
        logger.debug("Giving out Object ID Sequences to " + channelID + " from " + ids + " to "
                     + (ids + BATCH_SEQUENCE_SIZE));
        handshakeAck.initialize(ids, ids + BATCH_SEQUENCE_SIZE, persistent);
      } else {
        handshakeAck.initialize(0, 0, persistent);
      }

      // NOTE: handshake ack message send() must be done atomically with making the channel active
      // and is thus done inside this channel manager call
      channelManager.makeChannelActive(handshakeAck.getChannel(), handshakeAck);

    } catch (NoSuchChannelException e) {
      logger.warn("Not sending handshake message to disconnected client: " + channelID);
    }
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

  private void start() {
    logger.info("Starting DSO services...");
    lockManager.start();
    objectManager.start();
    for (Iterator i = channelManager.getRawChannelIDs().iterator(); i.hasNext();) {
      ChannelID channelID = (ChannelID) i.next();
      sendAckMessageFor(channelID);
    }
    state = STARTED;
  }

  private synchronized void setStarting() {
    assertInit();
    state = STARTING;
    logger.info("Starting reconnect window: " + this.reconnectTimeout + " ms.");
    timer.schedule(reconnectTimerTask, this.reconnectTimeout);
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

  public class NullClientHandshakeAckMessage implements ClientHandshakeAckMessage {

    public MessageChannel getChannel() {
      throw new UnsupportedOperationException();
    }

    public long getObjectIDSequenceEnd() {
      throw new UnsupportedOperationException();
    }

    public long getObjectIDSequenceStart() {
      throw new UnsupportedOperationException();
    }

    public boolean getPersistentServer() {
      throw new UnsupportedOperationException();
    }

    public void initialize(long start, long end, boolean p) {
      throw new UnsupportedOperationException();
    }

    public void send() {
      //
    }

  }

}
