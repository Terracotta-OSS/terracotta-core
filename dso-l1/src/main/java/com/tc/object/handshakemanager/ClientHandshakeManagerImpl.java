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

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
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

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientHandshakeManagerImpl implements ClientHandshakeManager {
  private enum State {
    PAUSED, STARTING, RUNNING
  }

  private static final TCLogger CONSOLE_LOGGER = CustomerLogging.getConsoleLogger();

  private final Collection<ClientHandshakeCallback> callBacks;
  private final ClientHandshakeMessageFactory chmf;
  private final TCLogger logger;
  private final SessionManager sessionManager;
  private final String clientVersion;

  private State state;
  private volatile boolean disconnected;
  private volatile boolean serverIsPersistent = false;
  private volatile boolean isShutdown = false;

  private final Lock lock = new ReentrantLock();
  private final ClusterInternalEventsGun clusterEventsGun;

  public ClientHandshakeManagerImpl(TCLogger logger, ClientHandshakeMessageFactory chmf,
                                    SessionManager sessionManager, ClusterInternalEventsGun clusterEventsGun, String clientVersion,
                                    Collection<ClientHandshakeCallback> callbacks) {
    this.logger = logger;
    this.chmf = chmf;
    this.sessionManager = sessionManager;
    this.clusterEventsGun = clusterEventsGun;
    this.clientVersion = clientVersion;
    this.callBacks = callbacks;
    this.state = State.PAUSED;
    this.disconnected = true;
    pauseCallbacks();
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

  private void initiateHandshake() {
    this.logger.debug("Initiating handshake...");
    ClientHandshakeMessage handshakeMessage;
    lock.lock();
    try {
      changeToStarting();
      handshakeMessage = this.chmf.newClientHandshakeMessage(this.clientVersion, isEnterpriseClient());
      notifyCallbackOnHandshake(handshakeMessage);
    } finally {
      lock.unlock();
    }
    this.logger.info("Sending handshake message");
    handshakeMessage.send();
  }

  protected boolean isEnterpriseClient() {
    return false;
  }

  @Override
  public void fireNodeError() {
    final String msg = "Reconnection was rejected from server. This client will never be able to join the cluster again.";
    logger.error(msg);
    CONSOLE_LOGGER.error(msg);
    clusterEventsGun.fireNodeError();
  }

  @Override
  public void disconnected() {
    // We ignore the disconnected call if we are shutting down.
    if (!checkShutdown()) {
      lock.lock();
      try {
        boolean isPaused = changeToPaused();
        if (isPaused) {
          pauseCallbacks();
          this.sessionManager.newSession();
          this.logger.info("ClientHandshakeManager moves to " + this.sessionManager.getSessionID());
        }
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  public void connected() {
    this.logger.info("Connected: Unpausing from " + getState());
    if (getState() != State.PAUSED) {
      this.logger.warn("Ignoring unpause while " + getState());
    } else if (!checkShutdown()) {
      // drop handshaking if shutting down
      initiateHandshake();
    }
  }

  @Override
  public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck) {
    acknowledgeHandshake(handshakeAck.getPersistentServer(), handshakeAck.getThisNodeId(), handshakeAck.getAllNodes(),
        handshakeAck.getServerVersion());
  }

  protected void acknowledgeHandshake(boolean persistentServer, ClientID thisNodeId, ClientID[] clusterMembers, String serverVersion) {
    this.logger.info("Received Handshake ack");
    if (getState() != State.STARTING) {
      this.logger.warn("Ignoring handshake acknowledgement while " + getState());
    } else {
      checkClientServerVersionCompatibility(serverVersion);
      this.serverIsPersistent = persistentServer;
      lock.lock();
      try {
        changeToRunning();
        unpauseCallbacks();
      } finally {
        lock.unlock();
      }

      clusterEventsGun.fireThisNodeJoined(thisNodeId, clusterMembers);
    }
  }

  protected void checkClientServerVersionCompatibility(String serverVersion) {
    final boolean check = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.VERSION_COMPATIBILITY_CHECK);

    if (check && !new VersionCompatibility().isCompatibleClientServer(new Version(clientVersion), new Version(serverVersion))) {
      final String msg = "Client/Server versions are not compatibile: Client Version: " + clientVersion + ", Server Version: "
                         + serverVersion + ".  Terminating client now.";
      CONSOLE_LOGGER.error(msg);
      throw new IllegalStateException(msg);
    }
  }

  private void shutdownCallbacks(boolean fromShutdownHook) {
    // Now that the handshake manager has concluded that it is entering into a shutdown state, anyone else wishing to use it
    // needs to be notified that they cannot.
    for (ClientHandshakeCallback c : this.callBacks) {
      c.shutdown(fromShutdownHook);
    }
  }

  private void pauseCallbacks() {
    for (ClientHandshakeCallback c : this.callBacks) {
      c.pause();
    }
  }

  private void notifyCallbackOnHandshake(ClientHandshakeMessage handshakeMessage) {
    for (ClientHandshakeCallback c : this.callBacks) {
      c.initializeHandshake(handshakeMessage);
    }
  }

  private void unpauseCallbacks() {
    for (ClientHandshakeCallback c : this.callBacks) {
      c.unpause();
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
      while (this.disconnected) {
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
  private synchronized boolean changeToPaused() {
    final State old = this.state;
    boolean didChangeToPaused = false;
    if (old != State.PAUSED) {
      this.state = State.PAUSED;
      didChangeToPaused = true;

      this.logger.info("Disconnected: Pausing from " + old + ". Disconnect count: " + this.disconnected);

      if (old == State.RUNNING) {
        this.disconnected = true;
        // A thread might be waiting for us to change whether or not we are disconnected.
        notifyAll();
      }

      this.clusterEventsGun.fireOperationsDisabled();
    }
    return didChangeToPaused;
  }

  private synchronized void changeToStarting() {
    Assert.assertEquals(state, State.PAUSED);
    state = State.STARTING;
  }

  private synchronized void changeToRunning() {
    Assert.assertEquals(state, State.STARTING);
    state = State.RUNNING;

    this.disconnected = false;
    notifyAll();
  }

  private synchronized State getState() {
    return this.state;
  }
}
