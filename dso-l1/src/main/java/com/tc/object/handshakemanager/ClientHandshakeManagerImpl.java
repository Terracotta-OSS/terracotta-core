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
package com.tc.object.handshakemanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.ClientID;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Util;
import com.tc.util.version.Version;
import com.tc.util.version.VersionCompatibility;
import com.tcclient.cluster.ClusterInternalEventsGun;

/**
 * This class has been changed to be heavily synchronized. This is in attempt to 
 * address a rare bug where handshake is not initiated properly because the state 
 * of the handshake manager is not properly protected.  The problem is that callbacks 
 * are made from within synchronized blocks as well, also to insure state is preserved 
 * during the entire stretch of the call.  This can be dangerous as the callback sites may 
 * lead to synchronization of their own, making this code prone to deadlocks.
 * 
 * TODO:  Constrain callback code to include as little synchronization as is safe.
 * @author 
 */
public class ClientHandshakeManagerImpl implements ClientHandshakeManager {
  private enum State {
    PAUSED, STARTING, RUNNING
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandshakeManagerImpl.class);

  private final ClientHandshakeCallback callBacks;
  private final boolean diagnostic;
  private final ClientHandshakeMessageFactory chmf;
  private final Logger logger;
  private final SessionManager sessionManager;
  private final String clientVersion;
  
  private final String   uuid;
  private final String name;

  private State state;
  private volatile boolean disconnected;
  private volatile boolean isShutdown = false;

  private final ClusterInternalEventsGun clusterEventsGun;

  public ClientHandshakeManagerImpl(Logger logger, ClientHandshakeMessageFactory chmf,
                                    SessionManager sessionManager, ClusterInternalEventsGun clusterEventsGun, 
                                    String uuid, String name, String clientVersion,
                                    ClientHandshakeCallback entities, boolean diagnostic) {
    this.logger = logger;
    this.chmf = chmf;
    this.sessionManager = sessionManager;
    this.clusterEventsGun = clusterEventsGun;
    this.uuid = uuid;
    this.name = name;
    this.clientVersion = clientVersion;
    this.callBacks = entities;
    this.diagnostic = diagnostic;
    this.state = State.PAUSED;
    this.disconnected = true;
    pauseCallbacks();
  }

  @Override
  public synchronized void shutdown(boolean fromShutdownHook) {
    isShutdown = true;
    notifyAll();
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

  private void initiateHandshake() {
    this.logger.debug("Initiating handshake...");
    ClientHandshakeMessage handshakeMessage;

    changeToStarting();
    handshakeMessage = this.chmf.newClientHandshakeMessage(this.uuid, this.name, this.clientVersion, isEnterpriseClient(), diagnostic);
    notifyCallbackOnHandshake(handshakeMessage);

    this.logger.debug("Sending handshake message");
    if (!handshakeMessage.send()) {
      if (handshakeMessage.getChannel().isConnected()) {
        LOGGER.error("handshake not sent but channel is connected", new Exception("FATAL HANDSHAKE ERROR"));
      } else {
        LOGGER.info("handshake failed. channel not connected");
      }
    }
  }

  protected boolean isEnterpriseClient() {
    return false;
  }

  @Override
  public void fireNodeError() {
    final String msg = "Reconnection was rejected from server. This client will never be able to join the cluster again.";
    logger.error(msg);
    LOGGER.error(msg);
    clusterEventsGun.fireNodeError();
  }

  @Override
  public synchronized void disconnected() {
    // We ignore the disconnected call if we are shutting down.
    if (!checkShutdown()) {
      boolean isPaused = changeToPaused();

      if (isPaused) {
      // A thread might be waiting for us to change whether or not we are disconnected.
        notifyAll();
        pauseCallbacks();
        this.sessionManager.newSession();
        this.logger.debug("ClientHandshakeManager moves to " + this.sessionManager.getSessionID());
      }
    }
  }

  @Override
  public synchronized void connected() {
    this.logger.debug("Connected: Unpausing from " + getState());
    if (getState() != State.PAUSED) {
      this.logger.warn("Ignoring unpause while " + getState());
    } else if (!checkShutdown()) {
      // drop handshaking if shutting down
      initiateHandshake();
    }
  }

  @Override
  public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck) {
    acknowledgeHandshake(handshakeAck.getThisNodeId(), handshakeAck.getAllNodes(),
        handshakeAck.getServerVersion());
  }

  protected synchronized void acknowledgeHandshake(ClientID thisNodeId, ClientID[] clusterMembers, String serverVersion) {
    this.logger.debug("Received Handshake ack");
    if (getState() != State.STARTING) {
      this.logger.warn("Ignoring handshake acknowledgement while " + getState());
    } else {
      checkClientServerVersionCompatibility(serverVersion);

      changeToRunning();
      notifyAll();
      unpauseCallbacks();

      clusterEventsGun.fireThisNodeJoined(thisNodeId, clusterMembers);
    }
  }

  protected void checkClientServerVersionCompatibility(String serverVersion) {
    final boolean check = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.VERSION_COMPATIBILITY_CHECK);

    if (check && !new VersionCompatibility().isCompatibleClientServer(new Version(clientVersion), new Version(serverVersion))) {
      final String msg = "Client/Server versions are not compatible: Client Version: " + clientVersion + ", Server Version: "
                         + serverVersion + ".  Terminating client now.";
      LOGGER.error(msg);
      throw new IllegalStateException(msg);
    }
  }

  private void shutdownCallbacks(boolean fromShutdownHook) {
    // Now that the handshake manager has concluded that it is entering into a shutdown state, anyone else wishing to use it
    // needs to be notified that they cannot.
    this.callBacks.shutdown(fromShutdownHook);
  }

  private void pauseCallbacks() {
    this.callBacks.pause();
  }

  private void notifyCallbackOnHandshake(ClientHandshakeMessage handshakeMessage) {
    this.callBacks.initializeHandshake(handshakeMessage);
  }

  private void unpauseCallbacks() {
    this.callBacks.unpause();
  }

  @Override
  public synchronized void waitForHandshake() {
    boolean isInterrupted = false;
    try {
      while (this.disconnected && !this.isShutdown()) {
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
  private boolean changeToPaused() {
    final State old = this.state;
    boolean didChangeToPaused = false;
    if (old != State.PAUSED) {
      this.state = State.PAUSED;
      didChangeToPaused = true;

      this.logger.debug("Disconnected: Pausing from " + old + ". Disconnect count: " + this.disconnected);

      if (old == State.RUNNING) {
        this.disconnected = true;
      }

      this.clusterEventsGun.fireOperationsDisabled();
    }
    return didChangeToPaused;
  }

  private void changeToStarting() {
    Assert.assertEquals(state, State.PAUSED);
    state = State.STARTING;
  }

  private void changeToRunning() {
    Assert.assertEquals(state, State.STARTING);
    state = State.RUNNING;

    this.disconnected = false;
  }

  private State getState() {
    return this.state;
  }
}
