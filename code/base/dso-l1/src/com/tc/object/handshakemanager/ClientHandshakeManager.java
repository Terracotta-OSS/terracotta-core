/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.cluster.Cluster;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.PauseListener;
import com.tc.object.RemoteObjectManager;
import com.tc.object.context.PauseContext;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.TryLockRequest;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.State;
import com.tc.util.Util;
import com.tc.util.sequence.BatchSequenceReceiver;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class ClientHandshakeManager implements ChannelEventListener {
  private static final State                   PAUSED             = new State("PAUSED");
  private static final State                   STARTING           = new State("STARTING");
  private static final State                   RUNNING            = new State("RUNNING");

  private final ClientObjectManager            objectManager;
  private final ClientLockManager              lockManager;
  private final ChannelIDProvider              cidp;
  private final ClientHandshakeMessageFactory  chmf;
  private final RemoteObjectManager            remoteObjectManager;
  private final ClientGlobalTransactionManager gtxManager;
  private final TCLogger                       logger;
  private final Collection                     stagesToPauseOnDisconnect;
  private final Sink                           pauseSink;
  private final SessionManager                 sessionManager;
  private final PauseListener                  pauseListener;
  private final BatchSequenceReceiver          sequenceReceiver;
  private final Cluster                        cluster;
  private final String                         clientVersion;

  private State                                state              = PAUSED;
  private boolean                              stagesPaused       = false;
  private boolean                              serverIsPersistent = false;

  public ClientHandshakeManager(TCLogger logger, ChannelIDProvider cidp, ClientHandshakeMessageFactory chmf,
                                ClientObjectManager objectManager, RemoteObjectManager remoteObjectManager,
                                ClientLockManager lockManager, RemoteTransactionManager remoteTransactionManager,
                                ClientGlobalTransactionManager gtxManager, Collection stagesToPauseOnDisconnect,
                                Sink pauseSink, SessionManager sessionManager, PauseListener pauseListener,
                                BatchSequenceReceiver sequenceReceiver, Cluster cluster, String clientVersion) {
    this.logger = logger;
    this.cidp = cidp;
    this.chmf = chmf;
    this.objectManager = objectManager;
    this.remoteObjectManager = remoteObjectManager;
    this.lockManager = lockManager;
    this.gtxManager = gtxManager;
    this.stagesToPauseOnDisconnect = stagesToPauseOnDisconnect;
    this.pauseSink = pauseSink;
    this.sessionManager = sessionManager;
    this.pauseListener = pauseListener;
    this.sequenceReceiver = sequenceReceiver;
    this.cluster = cluster;
    this.clientVersion = clientVersion;
    pauseManagers();
  }

  public void initiateHandshake() {
    logger.debug("Initiating handshake...");
    changeState(STARTING);
    notifyManagersStarting();

    ClientHandshakeMessage handshakeMessage = chmf.newClientHandshakeMessage();

    handshakeMessage.setClientVersion(clientVersion);

    handshakeMessage.setTransactionSequenceIDs(gtxManager.getTransactionSequenceIDs());
    handshakeMessage.setResentTransactionIDs(gtxManager.getResentTransactionIDs());

    logger.debug("Getting object ids...");
    for (Iterator i = objectManager.getAllObjectIDsAndClear(new HashSet()).iterator(); i.hasNext();) {
      handshakeMessage.addObjectID((ObjectID) i.next());
    }

    logger.debug("Getting lock holders...");
    for (Iterator i = lockManager.addAllHeldLocksTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), cidp.getChannelID(), request.threadID(), request.lockLevel());
      handshakeMessage.addLockContext(ctxt);
    }

    logger.debug("Getting lock waiters...");
    for (Iterator i = lockManager.addAllWaitersTo(new HashSet()).iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      WaitContext ctxt = new WaitContext(request.lockID(), cidp.getChannelID(), request.threadID(),
                                         request.lockLevel(), request.getWaitInvocation());
      handshakeMessage.addWaitContext(ctxt);
    }

    logger.debug("Getting pending lock requests...");
    for (Iterator i = lockManager.addAllPendingLockRequestsTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), cidp.getChannelID(), request.threadID(), request.lockLevel());
      handshakeMessage.addPendingLockContext(ctxt);
    }

    logger.debug("Getting pending tryLock requests...");
    for (Iterator i = lockManager.addAllPendingTryLockRequestsTo(new HashSet()).iterator(); i.hasNext();) {
      TryLockRequest request = (TryLockRequest) i.next();
      LockContext ctxt = new TryLockContext(request.lockID(), cidp.getChannelID(), request.threadID(), request
          .lockLevel(), request.getWaitInvocation());
      handshakeMessage.addPendingTryLockContext(ctxt);
    }

    logger.debug("Checking to see if is object ids sequence is needed ...");
    handshakeMessage.setIsObjectIDsRequested(!sequenceReceiver.hasNext());

    logger.debug("Sending handshake message...");
    handshakeMessage.send();
  }

  public void notifyChannelEvent(ChannelEvent event) {
    if (event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT) {
      cluster.thisNodeDisconnected();
      pauseSink.add(PauseContext.PAUSE);
    } else if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
      pauseSink.add(PauseContext.UNPAUSE);
    } else if (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT) {
      cluster.thisNodeDisconnected();
    }
  }

  public void pause() {
    logger.info("Pause " + getState());
    if (getState() == PAUSED) {
      logger.warn("pause called while already PAUSED");
      return;
    }
    pauseStages();
    pauseManagers();
    changeState(PAUSED);
    // all the activities paused then can switch to new session
    sessionManager.newSession();
  }

  public void unpause() {
    logger.info("Unpause " + getState());
    if (getState() != PAUSED) {
      logger.warn("unpause called while not PAUSED: " + getState());
      return;
    }
    unpauseStages();
    initiateHandshake();
  }

  public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck) {
    acknowledgeHandshake(handshakeAck.getObjectIDSequenceStart(), handshakeAck.getObjectIDSequenceEnd(), handshakeAck
        .getPersistentServer(), handshakeAck.getThisNodeId(), handshakeAck.getAllNodes(), handshakeAck
        .getServerVersion());
  }

  protected void acknowledgeHandshake(long objectIDStart, long objectIDEnd, boolean persistentServer,
                                      String thisNodeId, String[] clusterMembers, String serverVersion) {
    if (getState() != STARTING) {
      logger.warn("Handshake acknowledged while not STARTING: " + getState());
      return;
    }

    final boolean checkVersionMatches = TCPropertiesImpl.getProperties()
        .getBoolean("l1.connect.versionMatchCheck.enabled");
    if (checkVersionMatches) {
      checkClientServerVersionMatch(logger, clientVersion, serverVersion);
    }

    this.serverIsPersistent = persistentServer;

    cluster.thisNodeConnected(thisNodeId, clusterMembers);

    if (objectIDStart < objectIDEnd) {
      logger.debug("Setting the ObjectID sequence to: " + objectIDStart + " , " + objectIDEnd);
      sequenceReceiver.setNextBatch(objectIDStart, objectIDEnd);
    }

    logger.debug("Re-requesting outstanding object requests...");
    remoteObjectManager.requestOutstanding();

    logger.debug("Handshake acknowledged.  Resending incomplete transactions...");
    gtxManager.resendOutstandingAndUnpause();
    unpauseManagers();

    changeState(RUNNING);
  }

  protected static void checkClientServerVersionMatch(TCLogger logger, String clientVersion, String serverVersion) {
    if (!clientVersion.equals(serverVersion)) {
      final String msg = "Client/Server Version Mismatch Error: Client Version: " + clientVersion
                         + ", Server Version: " + serverVersion + ".  Terminating client now.";
      throw new RuntimeException(msg);
    }
  }

  private void pauseManagers() {
    lockManager.pause();
    objectManager.pause();
    remoteObjectManager.pause();
    gtxManager.pause();
    pauseListener.notifyPause();
  }

  private void notifyManagersStarting() {
    lockManager.starting();
    objectManager.starting();
    remoteObjectManager.starting();
    gtxManager.starting();
  }

  // XXX:: Note that gtxmanager is actually unpaused outside this method as it
  // has to resend transactions and unpause in a single step.
  private void unpauseManagers() {
    lockManager.unpause();
    objectManager.unpause();
    remoteObjectManager.unpause();
    pauseListener.notifyUnpause();
  }

  private void pauseStages() {
    if (!stagesPaused) {
      logger.debug("Pausing stages...");
      for (Iterator i = stagesToPauseOnDisconnect.iterator(); i.hasNext();) {
        ((Stage) i.next()).pause();
      }
      stagesPaused = true;
    } else {
      logger.debug("pauseStages(): Stages are paused; not pausing stages.");
    }
  }

  private void unpauseStages() {
    if (stagesPaused) {
      logger.debug("Unpausing stages...");
      for (Iterator i = stagesToPauseOnDisconnect.iterator(); i.hasNext();) {
        ((Stage) i.next()).unpause();
      }
      stagesPaused = false;
    } else {
      logger.debug("unpauseStages(): Stages not paused; not unpausing stages.");
    }
  }

  /**
   *
   */
  public boolean serverIsPersistent() {
    return this.serverIsPersistent;
  }

  public synchronized void waitForHandshake() {
    boolean isInterrupted = false;
    while (state != RUNNING) {
      try {
        wait();
      } catch (InterruptedException e) {
        logger.error("Interrupted while waiting for handshake");
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  private synchronized void changeState(State newState) {
    state = newState;
    notifyAll();
  }

  private synchronized State getState() {
    return state;
  }

}
