/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.StripeID;
import com.tc.object.ClearableCallback;
import com.tc.object.ClientIDProvider;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Util;
import com.tc.util.version.Version;
import com.tc.util.version.VersionCompatibility;
import com.tcclient.cluster.DsoClusterInternalEventsGun;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientHandshakeManagerImpl implements ClientHandshakeManager {
  private static enum State {
    PAUSED, STARTING, RUNNING
  }

  private static final TCLogger                     CONSOLE_LOGGER       = CustomerLogging.getConsoleLogger();

  private final Collection<ClientHandshakeCallback> callBacks;
  private final ClientIDProvider                    cidp;
  private final ClientHandshakeMessageFactory       chmf;
  private final TCLogger                            logger;
  private final SessionManager                      sessionManager;
  private final String                              clientVersion;
  private final Map                                 groupStates          = new HashMap();
  private final GroupID[]                           groupIDs;
  private volatile int                              disconnected;
  private volatile boolean                          serverIsPersistent   = false;
  private volatile boolean                          isShutdown           = false;
  private final Lock                                lock                 = new ReentrantLock();
  private final DsoClusterInternalEventsGun         dsoClusterEventsGun;
  private final Collection<ClearableCallback>       clearCallbacks;
  protected Map<GroupID, StripeID>                  groupIDToStripeIDMap = new HashMap<GroupID, StripeID>();
  private boolean                                   isMapReceived        = false;

  public ClientHandshakeManagerImpl(final TCLogger logger, final DSOClientMessageChannel channel,
                                    final ClientHandshakeMessageFactory chmf, final Sink pauseSink,
                                    final SessionManager sessionManager,
                                    final DsoClusterInternalEventsGun dsoClusterEventsGun, final String clientVersion,
                                    final Collection<ClientHandshakeCallback> callbacks,
                                    final Collection<ClearableCallback> clearCallbacks) {
    this.logger = logger;
    this.cidp = channel.getClientIDProvider();
    this.chmf = chmf;
    this.sessionManager = sessionManager;
    this.dsoClusterEventsGun = dsoClusterEventsGun;
    this.clientVersion = clientVersion;
    this.callBacks = callbacks;
    this.groupIDs = channel.getGroupIDs();
    this.disconnected = this.groupIDs.length;
    initGroupStates(State.PAUSED);
    pauseCallbacks(GroupID.ALL_GROUPS, this.disconnected);
    this.clearCallbacks = clearCallbacks;
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    isShutdown = true;
    shutdownCallbacks(fromShutdownHook);
  }

  @Override
  public boolean isShutdown() {
    return isShutdown;
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

  @Override
  public void initiateHandshake(final NodeID remoteNode) {
    this.logger.debug("Initiating handshake...");
    ClientHandshakeMessage handshakeMessage;
    lock.lock();
    try {
      changeToStarting(remoteNode);
      handshakeMessage = this.chmf.newClientHandshakeMessage(remoteNode, this.clientVersion, isEnterpriseClient());
      notifyCallbackOnHandshake(remoteNode, handshakeMessage);
    } finally {
      lock.unlock();
    }
    this.logger.info("Sending handshake message with oids " + handshakeMessage.getObjectIDs().size() + " validate "
                     + handshakeMessage.getObjectIDsToValidate().size());
    handshakeMessage.send();
  }

  protected boolean isEnterpriseClient() {
    return false;
  }

  private synchronized boolean isOnlyOneGroupDisconnected() {
    return 1 == this.disconnected;
  }

  @Override
  public void fireNodeErrorIfNecessary(boolean rejoinEnabled) {
    if (!rejoinEnabled) {
      final String msg = "Reconnection was rejected from server, but rejoin is not enabled. This client will never be able to join the cluster again";
      logger.error(msg);
      CONSOLE_LOGGER.error(msg);
      dsoClusterEventsGun.fireNodeError();
    }
  }

  @Override
  public void disconnected(final NodeID remoteNode) {
    if (checkShutdown()) return;
    lock.lock();
    try {
      boolean isPaused = changeToPaused(remoteNode);
      if (isPaused) {
        pauseCallbacks(remoteNode, getDisconnectedCount());
        this.sessionManager.newSession(remoteNode);
        this.logger.info("ClientHandshakeManager moves to " + this.sessionManager.getSessionID(remoteNode)
                         + " for remote node " + remoteNode);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void connected(final NodeID remoteNode) {
    this.logger.info("Connected: Unpausing from " + getState(remoteNode) + " RemoteNode : " + remoteNode
                     + ". Disconnect count : " + getDisconnectedCount());
    if (getState(remoteNode) != State.PAUSED) {
      this.logger.warn("Ignoring unpause while " + getState(remoteNode) + " for " + remoteNode);
      return;
    }
    // drop handshaking if shutting down
    if (checkShutdown()) return;
    initiateHandshake(remoteNode);
  }

  private synchronized void receivedStripeIDMap(Map<GroupID, StripeID> map) {
    if (!this.isMapReceived) {
      this.groupIDToStripeIDMap = Collections.unmodifiableMap(map);
      this.isMapReceived = true;
    }
  }

  protected void verifyActiveCoordinator(GroupID groupId) {
    // Overridden in sub-class ClientGroupHandshakeManagerImpl
  }

  @Override
  public void acknowledgeHandshake(final ClientHandshakeAckMessage handshakeAck) {
    Map<GroupID, StripeID> stripeIDMap = handshakeAck.getStripeIDMap();
    logger.info("Received StripeIDMap size:" + stripeIDMap.size() + " from " + handshakeAck.getGroupID());
    if (stripeIDMap.size() > 0) {
      verifyActiveCoordinator(handshakeAck.getGroupID());
      receivedStripeIDMap(stripeIDMap);
      if (!groupIDToStripeIDMap.equals(stripeIDMap)) {
        final String msg = "Client can not join a new cluster before shutdown \n original " + groupIDToStripeIDMap
                           + " \n received " + stripeIDMap;
        logger.error(msg);
        CONSOLE_LOGGER.error(msg);
        dsoClusterEventsGun.fireNodeError();
        return;
      }
    }
    acknowledgeHandshake(handshakeAck.getSourceNodeID(), handshakeAck.getPersistentServer(),
                         handshakeAck.getThisNodeId(), handshakeAck.getAllNodes(), handshakeAck.getServerVersion());
  }

  private synchronized boolean areAllGroupsConnected() {
    return 0 == this.disconnected;
  }

  protected void acknowledgeHandshake(final NodeID remoteID, final boolean persistentServer, final ClientID thisNodeId,
                                      final ClientID[] clusterMembers, final String serverVersion) {
    this.logger.info("Received Handshake ack from remote node :" + remoteID);
    if (getState(remoteID) != State.STARTING) {
      this.logger.warn("Ignoring handshake acknowledgement while " + getState(remoteID) + " for " + remoteID);
      return;
    }

    checkClientServerVersionCompatibility(serverVersion);
    this.serverIsPersistent = persistentServer;
    lock.lock();
    try {
      changeToRunning(remoteID);
      unpauseCallbacks(remoteID, getDisconnectedCount());
    } finally {
      lock.unlock();
    }
    // only send out out these events when no groups are paused anymore
    if (areAllGroupsConnected()) {
      // first node joined event will also fire ops enabled event.
      dsoClusterEventsGun.fireThisNodeJoined(thisNodeId, clusterMembers);
    }
  }

  protected void checkClientServerVersionCompatibility(final String serverVersion) {
    final boolean check = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.VERSION_COMPATIBILITY_CHECK);

    if (check && !new VersionCompatibility().isCompatibleClientServer(new Version(clientVersion), new Version(serverVersion))) {
      final String msg = "Client/Server versions are not compatibile: Client Version: " + clientVersion
                         + ", Server Version: " + serverVersion + ".  Terminating client now.";
      CONSOLE_LOGGER.error(msg);
      throw new IllegalStateException(msg);
    }
  }

  private void shutdownCallbacks(boolean fromShutdownHook) {
    for (ClientHandshakeCallback c : this.callBacks) {
      c.shutdown(fromShutdownHook);
    }
  }

  @Override
  public void reset() {
    // clean up all L1 stages and other ClearableCallback which are not ClientHandshakeCallback
    for (ClearableCallback clearable : clearCallbacks) {
      clearable.cleanup();
    }

    logger.info("reconnection rejected or closed from server, disconnecting all groups");
    for (GroupID groupId : groupIDToStripeIDMap.keySet()) {
      disconnected(groupId);
    }

    // clean up ClientHandshakeCallback (all L1 managers) and other ClearableCallback
    for (ClientHandshakeCallback c : callBacks) {
      c.cleanup();
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
      this.logger.debug(c.getClass().getName() + " oids " + handshakeMessage.getObjectIDs().size() + " validate "
                        + handshakeMessage.getObjectIDsToValidate().size());
    }
  }

  private void unpauseCallbacks(final NodeID remote, final int disconnectedCount) {
    for (ClientHandshakeCallback c : this.callBacks) {
      c.unpause(remote, disconnectedCount);
    }
  }

  @Override
  public boolean serverIsPersistent() {
    return this.serverIsPersistent;
  }

  @Override
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

  // returns true if PAUSED else return false if already PAUSED
  private synchronized boolean changeToPaused(final NodeID node) {
    Object old = this.groupStates.put(node, State.PAUSED);

    if (old == State.PAUSED) { return false; }

    this.logger.info("Disconnected: Pausing from " + old + " RemoteNode : " + node + ". Disconnect count: "
                     + disconnected);

    if (old == State.RUNNING) {
      this.disconnected++;
    }

    if (this.disconnected > this.groupIDs.length) { throw new AssertionError(
                                                                             "disconnected count was greater then number of groups ( "
                                                                                 + this.groupIDs.length + " ) , "
                                                                                 + " disconnected = "
                                                                                 + this.disconnected); }
    notifyAll();
    // only send the operations disabled event when this was the first group to disconnect
    if (isOnlyOneGroupDisconnected()) {
      dsoClusterEventsGun.fireOperationsDisabled();
    }
    return true;
  }

  private synchronized void changeToStarting(final NodeID node) {
    Object old = this.groupStates.put(node, State.STARTING);
    Assert.assertEquals(old, State.PAUSED);
  }

  private synchronized void changeToRunning(final NodeID node) {
    Object old = this.groupStates.put(node, State.RUNNING);
    Assert.assertEquals(old, State.STARTING);
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
