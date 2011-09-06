/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.object.ClientIDProvider;
import com.tc.object.context.PauseContext;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.Util;
import com.tcclient.cluster.DsoClusterInternalEventsGun;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandshakeManagerImpl implements ClientHandshakeManager, ChannelEventListener {
  private static final State                        PAUSED               = new State("PAUSED");
  private static final State                        STARTING             = new State("STARTING");
  private static final State                        RUNNING              = new State("RUNNING");
  private static final TCLogger                     CONSOLE_LOGGER       = CustomerLogging.getConsoleLogger();

  private final Collection<ClientHandshakeCallback> callBacks;
  private final ClientIDProvider                    cidp;
  private final ClientHandshakeMessageFactory       chmf;
  private final TCLogger                            logger;
  private final Sink                                pauseSink;
  private final SessionManager                      sessionManager;
  private final String                              clientVersion;
  private final Map                                 groupStates          = new HashMap();
  private final GroupID[]                           groupIDs;
  private volatile int                              disconnected;
  private volatile boolean                          serverIsPersistent   = false;
  private volatile boolean                          isShutdown           = false;
  private final AtomicBoolean                       transitionInProgress = new AtomicBoolean(false);
  private final DsoClusterInternalEventsGun         dsoClusterEventsGun;

  public ClientHandshakeManagerImpl(final TCLogger logger, final DSOClientMessageChannel channel,
                                    final ClientHandshakeMessageFactory chmf, final Sink pauseSink,
                                    final SessionManager sessionManager,
                                    final DsoClusterInternalEventsGun dsoClusterEventsGun, final String clientVersion,
                                    final Collection<ClientHandshakeCallback> callbacks) {
    this.logger = logger;
    this.cidp = channel.getClientIDProvider();
    this.chmf = chmf;
    this.pauseSink = pauseSink;
    this.sessionManager = sessionManager;
    this.dsoClusterEventsGun = dsoClusterEventsGun;
    this.clientVersion = clientVersion;
    this.callBacks = callbacks;
    this.groupIDs = channel.getGroupIDs();
    this.disconnected = this.groupIDs.length;
    initGroupStates(PAUSED);
    pauseCallbacks(GroupID.ALL_GROUPS, this.disconnected);
  }

  private void waitForTransitionToComplete() {
    while (transitionInProgress.get()) {
      try {
        transitionInProgress.wait();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  private void notifyTransitionComplete() {
    synchronized (transitionInProgress) {
      transitionInProgress.set(false);
      transitionInProgress.notifyAll();
    }
  }

  public void shutdown() {
    isShutdown = true;
    shutdownCallbacks();
  }

  private boolean checkShutdown() {
    if (isShutdown) {
      this.logger.warn("Drop handshaking due to client shutting down...");
    }
    return isShutdown;
  }

  private synchronized void initGroupStates(final State state) {
    for (GroupID groupID : this.groupIDs) {
      this.groupStates.put(groupID, state);
    }
  }

  public void initiateHandshake(final NodeID remoteNode) {
    this.logger.debug("Initiating handshake...");
    synchronized (transitionInProgress) {
      waitForTransitionToComplete();
      transitionInProgress.set(true);
    }
    changeToStarting(remoteNode);

    ClientHandshakeMessage handshakeMessage = this.chmf.newClientHandshakeMessage(remoteNode, this.clientVersion,
                                                                                  enterpriseClient());
    notifyCallbackOnHandshake(remoteNode, handshakeMessage);
    notifyTransitionComplete();

    this.logger.debug("Sending handshake message...");
    handshakeMessage.send();
  }

  protected boolean enterpriseClient() {
    return false;
  }

  public void notifyChannelEvent(final ChannelEvent event) {
    if (GroupID.ALL_GROUPS.equals(event.getChannel().getRemoteNodeID())) { throw new AssertionError(
                                                                                                    "Recd event for Group Channel : "
                                                                                                        + event); }
    if (event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT) {
      this.pauseSink.add(new PauseContext(true, event.getChannel().getRemoteNodeID()));
    } else if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
      this.pauseSink.add(new PauseContext(false, event.getChannel().getRemoteNodeID()));
    } else if (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT) {
      // ReconnectionRejectedListenerImpl is anyway firing thisNodeLeft on Channel Close. Below one seems to be
      // redundant.
      // if (!isShutdown) {
      // dsoClusterEventsGun.fireOperationsDisabled();
      // }
    }
  }

  private synchronized boolean isOnlyOneGroupDisconnected() {
    return 1 == this.disconnected;
  }

  public void disconnected(final NodeID remoteNode) {
    if (checkShutdown()) return;
    State currentState = getState(remoteNode);
    if (currentState == PAUSED) {
      this.logger.warn("Pause called while already PAUSED for " + remoteNode);
    } else if (currentState == STARTING) {
      // can happen when we get server disconnects before ack for client handshake
      this.logger.info("Disconnected: Ignoring disconnect event from  RemoteNode : " + remoteNode
                       + " as the current state is " + currentState + ". Disconnect count: " + getDisconnectedCount());
      // Atomize manager and callbacks state changes
      synchronized (transitionInProgress) {
        waitForTransitionToComplete();
        transitionInProgress.set(true);
      }
      changeToPaused(remoteNode);
      pauseCallbacks(remoteNode, getDisconnectedCount());
      notifyTransitionComplete();

      this.sessionManager.newSession(remoteNode);
      this.logger.info("ClientHandshakeManager moves to " + this.sessionManager.getSessionID(remoteNode));

    } else {
      this.logger.info("Disconnected: Pausing from " + currentState + " RemoteNode : " + remoteNode
                       + ". Disconnect count: " + getDisconnectedCount());
      // Atomize manager and callbacks state changes
      synchronized (transitionInProgress) {
        waitForTransitionToComplete();
        transitionInProgress.set(true);
      }
      changeToPaused(remoteNode);
      pauseCallbacks(remoteNode, getDisconnectedCount());
      notifyTransitionComplete();
      // all the activities paused then can switch to new session
      this.sessionManager.newSession(remoteNode);
      this.logger.info("ClientHandshakeManager moves to " + this.sessionManager.getSessionID(remoteNode));

      // only send the operations disabled event when this was the first group to disconnect
      if (isOnlyOneGroupDisconnected()) {
        dsoClusterEventsGun.fireOperationsDisabled();
      }
    }
  }

  public void connected(final NodeID remoteNode) {
    this.logger.info("Connected: Unpausing from " + getState(remoteNode) + " RemoteNode : " + remoteNode
                     + ". Disconnect count : " + getDisconnectedCount());

    if (getState(remoteNode) != PAUSED) {
      this.logger.warn("Unpause called while not PAUSED for " + remoteNode);
      return;
    }
    // drop handshaking if shutting down
    if (checkShutdown()) return;
    initiateHandshake(remoteNode);
  }

  public void acknowledgeHandshake(final ClientHandshakeAckMessage handshakeAck) {
    acknowledgeHandshake(handshakeAck.getSourceNodeID(), handshakeAck.getPersistentServer(),
                         handshakeAck.getThisNodeId(), handshakeAck.getAllNodes(), handshakeAck.getServerVersion());
  }

  private synchronized boolean areAllGroupsConnected() {
    return 0 == this.disconnected;
  }

  protected void acknowledgeHandshake(final NodeID remoteID, final boolean persistentServer, final NodeID thisNodeId,
                                      final NodeID[] clusterMembers, final String serverVersion) {
    this.logger.info("Received Handshake ack for this node :" + remoteID);
    if (getState(remoteID) != STARTING) {
      this.logger.warn("Handshake acknowledged while not STARTING: " + getState(remoteID));
      return;
    }

    checkClientServerVersionMatch(serverVersion);
    this.serverIsPersistent = persistentServer;
    synchronized (transitionInProgress) {
      waitForTransitionToComplete();
      transitionInProgress.set(true);
    }
    changeToRunning(remoteID);
    unpauseCallbacks(remoteID, getDisconnectedCount());
    notifyTransitionComplete();

    // only send out out these events when no groups are paused anymore
    if (areAllGroupsConnected()) {
      // first node joined event will also fire ops enabled event.
      dsoClusterEventsGun.fireThisNodeJoined(thisNodeId, clusterMembers);
    }
  }

  protected void checkClientServerVersionMatch(final String serverVersion) {
    final boolean checkVersionMatches = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_CONNECT_VERSION_MATCH_CHECK);
    if (checkVersionMatches && !this.clientVersion.equals(serverVersion)) {
      final String msg = "Client/Server Version Mismatch Error: Client Version: " + this.clientVersion
                         + ", Server Version: " + serverVersion + ".  Terminating client now.";
      CONSOLE_LOGGER.error(msg);
      mismatchExitWay(msg);
    }
  }

  // to be override by test program
  protected void mismatchExitWay(String msg) {
    System.exit(-1);
  }

  private void shutdownCallbacks() {
    for (ClientHandshakeCallback c : this.callBacks) {
      c.shutdown();
    }
  }

  private void pauseCallbacks(final NodeID remote, final int disconnectedCount) {
    for (ClientHandshakeCallback c : this.callBacks) {
      c.pause(remote, disconnectedCount);
    }
  }

  private void notifyCallbackOnHandshake(final NodeID remote, final ClientHandshakeMessage handshakeMessage) {
    for (ClientHandshakeCallback c : this.callBacks) {
      c.initializeHandshake(this.cidp.getClientID(), remote, handshakeMessage);
    }
  }

  private void unpauseCallbacks(final NodeID remote, final int disconnectedCount) {
    for (ClientHandshakeCallback c : this.callBacks) {
      c.unpause(remote, disconnectedCount);
    }
  }

  public boolean serverIsPersistent() {
    return this.serverIsPersistent;
  }

  public synchronized void waitForHandshake() {
    boolean isInterrupted = false;
    try {
      while (this.disconnected != 0) {
        try {
          wait();
        } catch (InterruptedException e) {
          this.logger.error("Interrupted while waiting for handshake");
          isInterrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(isInterrupted);
    }
  }

  private synchronized void changeToPaused(final NodeID node) {
    Object old = this.groupStates.put(node, PAUSED);

    if (old == PAUSED) { throw new AssertionError("old value was already equal PAUSED"); }

    if (old == RUNNING) {
      this.disconnected++;
    }

    if (this.disconnected > this.groupIDs.length) { throw new AssertionError(
                                                                             "disconnected count was greater then number of groups ( "
                                                                                 + this.groupIDs.length + " ) , "
                                                                                 + " disconnected = "
                                                                                 + this.disconnected); }
    notifyAll();
  }

  private synchronized void changeToStarting(final NodeID node) {
    Object old = this.groupStates.put(node, STARTING);
    Assert.assertEquals(old, PAUSED);
  }

  private synchronized void changeToRunning(final NodeID node) {
    Object old = this.groupStates.put(node, RUNNING);
    Assert.assertEquals(old, STARTING);
    this.disconnected--;
    if (this.disconnected < 0) { throw new AssertionError("disconnected count is less than zero, disconnected = "
                                                          + this.disconnected); }
    notifyAll();
  }

  private synchronized State getState(final NodeID node) {
    return (State) this.groupStates.get(node);
  }

  private int getDisconnectedCount() {
    return this.disconnected;
  }
}
