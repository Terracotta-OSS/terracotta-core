/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.terracotta.monitoring.PlatformService.RestartMode;
import org.terracotta.monitoring.PlatformStopException;


/**
 * TODO: Better rename this class as PassthroughStripeControl?
 * The implementation used to control a passthrough testing cluster which can contain multiple servers
 */
public class PassthroughClusterControl implements IClusterControl {
  private final String stripeName;
  private List<PassthroughServer> passthroughServers = new ArrayList<PassthroughServer>();
  private List<PassthroughServer> stoppedPassthroughServers = new ArrayList<PassthroughServer>();
  // elected active server
  private PassthroughServer activeServer;
  // A spot to store the most recently active server is we brought it down with nothing else running.  This is so we can re-attach the clients, later.
  private PassthroughServer mostRecentlyStoppedActiveServer;
  private final PassthroughServerCrasher crasher;

  /**
   * Constructs a PassthroughClusterControl with given stripeName and servers (at least one server needed to define a
   * stripe)
   *
   * @param stripeName Stripe Name
   * @param passthroughServer A {@link PassthroughServer}
   * @param passthroughServers more {@link PassthroughServer}s
   */
  public PassthroughClusterControl(String stripeName, PassthroughServer passthroughServer, PassthroughServer... passthroughServers) {
    this.stripeName = stripeName;
    this.crasher = new PassthroughServerCrasher(this);
    this.crasher.start();
    Assert.assertTrue(passthroughServer != null);
    this.passthroughServers.add(passthroughServer);
    passthroughServer.registerAsynchronousServerCrasher(this.crasher);
    for (PassthroughServer ps : passthroughServers) {
      Assert.assertTrue(ps != null);
      this.passthroughServers.add(ps);
      ps.setClusterControl(this);
      ps.registerAsynchronousServerCrasher(this.crasher);
    }
    bootstrapCluster();
  }

  @Override
  public synchronized void waitForActive() throws Exception {
    while (this.activeServer == null) {
      this.wait();
    }
  }

  public void startOneServerWithConsistency() throws Exception {
    // this is bogus, passthrough does not know about consistency and maybe it shouldn't
    // TODO: re-evaluate at some later point
    this.startOneServer();
  }

  public void startAllServersWithConsistency() throws Exception {
    // this is bogus, passthrough does not know about consistency and maybe it shouldn't
    // TODO: re-evaluate at some later point
    this.startAllServers();
  }

  @Override
  public synchronized void waitForRunningPassivesInStandby() throws Exception {
    // we sync passives during the active election itself, so just wait on same state
    while (this.activeServer == null) {
      this.wait();
    }
  }

  @Override
  public synchronized void startOneServer() {
    internalStartOneServer();
  }

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value="IS2_INCONSISTENT_SYNC")
  private void internalStartOneServer() {
    try {
      PassthroughServer terminatedServer = this.stoppedPassthroughServers.remove(0);
      startTerminatedServer(terminatedServer);
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalStateException("There are no terminated servers to start");
    }
  }

  @Override
  public synchronized void startAllServers() {
    while (!this.stoppedPassthroughServers.isEmpty()) {
      PassthroughServer terminatedServer = this.stoppedPassthroughServers.remove(0);
      startTerminatedServer(terminatedServer);
    }
  }

  @Override
  public synchronized void terminateActive() {
    internalTerminateActive();
  }

  @Override
  public synchronized void terminateOnePassive() {
    // Find a passthrough server which is not active or already terminated.
    PassthroughServer victim = null;
    for (PassthroughServer candidate : this.passthroughServers) {
      if ((candidate != this.activeServer) && !this.stoppedPassthroughServers.contains(candidate)) {
        victim = candidate;
        break;
      }
    }
    if (null != victim) {
      internalTerminatePassive(victim);
    }
  }

  private void internalTerminatePassive(PassthroughServer victim) {
    // Disconnect it from the active.
    if (null != this.activeServer) {
      this.activeServer.detachDownstreamPassive(victim);
    }
    // Stop the server.
    victim.stop();
    // Add it to our stopped list so we don't consider it for election.
    this.stoppedPassthroughServers.add(victim);
  }

  @Override
  public synchronized void terminateAllServers() {
    // First, we will bring down the active, so we can stop the flow of messages through the system.
    if (null != this.activeServer) {
      // disconnect clients and stop current active
      this.activeServer.disconnectClients();
      this.activeServer.stop();
      // Unregister the stripe.
      Assert.assertTrue(PassthroughServerRegistry.getSharedInstance().unregisterServer(this.stripeName) == this.activeServer);
      // Add the server to the stopped list.
      this.stoppedPassthroughServers.add(this.activeServer);
      // Clear the active
      this.activeServer = null;
    }

    for (PassthroughServer candidate : this.passthroughServers) {
      if (!this.stoppedPassthroughServers.contains(candidate)) {
        // This server is still running so bring it down.
        candidate.stop();
        // Add it to the stopped list.
        this.stoppedPassthroughServers.add(candidate);
      }
    }
  }

  public synchronized void restartOneServerFromInside(PassthroughServerProcess victim) {
    // Walk all the servers, terminating the one with victim as the underlying process.
    for (PassthroughServer candidate : this.passthroughServers) {
      if (!this.stoppedPassthroughServers.contains(candidate)  && (candidate.isRunningProcess(victim))) {
        // Terminate this server.
        if (candidate == this.activeServer) {
          internalTerminateActive();
        } else {
          internalTerminatePassive(candidate);
        }
        // Now, restart it.
        internalStartOneServer();
        break;
      }
    }
  }

  public void tearDown() {
    this.crasher.waitForStop();
    PassthroughServer removedServer = PassthroughServerRegistry.getSharedInstance().unregisterServer(this.stripeName);
    Assert.assertTrue(this.activeServer == removedServer);
    for (PassthroughServer passthroughServer : passthroughServers) {
      // don't stop twice
      if(!stoppedPassthroughServers.contains(passthroughServer)) {
        passthroughServer.stop();
      }
    }
  }

  private synchronized void bootstrapCluster() {
    Assert.assertTrue(this.activeServer == null);

    final PassthroughServer electedActive = electActive();

    boolean isActive = true;
    boolean shouldStorageLoaded = false;

    electedActive.start(isActive, shouldStorageLoaded, Collections.<Long>emptySet());

    for (PassthroughServer passthroughServer : passthroughServers) {
      if(!electedActive.equals(passthroughServer)) {
        passthroughServer.start(!isActive, shouldStorageLoaded, Collections.<Long>emptySet());
      }
    }
    attachPassivesToActive(electedActive);

    PassthroughServer prevActive = PassthroughServerRegistry.getSharedInstance().registerServer(this.stripeName, electedActive);
    Assert.assertTrue(prevActive == null);

    this.activeServer = electedActive;
    this.notifyAll();
  }

  private PassthroughServer electActive() {
    // Create eligible servers list by excluding stopped servers
    List<PassthroughServer> eligibleServers = new ArrayList<PassthroughServer>();
    for (PassthroughServer passthroughServer : passthroughServers) {
      if(!stoppedPassthroughServers.contains(passthroughServer)) {
        eligibleServers.add(passthroughServer);
      }
    }

    PassthroughServer activeServer = null;
    if(eligibleServers.size() > 0) {
      int random = (int) (Math.random() * eligibleServers.size());
      activeServer = eligibleServers.get(random);
    }

    return activeServer;
  }

  private void attachPassivesToActive(PassthroughServer activeServer) {
    for (PassthroughServer passthroughServer : passthroughServers) {
      if(!stoppedPassthroughServers.contains(passthroughServer) && !activeServer.equals(passthroughServer)) {
        activeServer.attachDownstreamPassive(passthroughServer);
      }
    }
  }

  private void startTerminatedServer(PassthroughServer lastTerminatedServer) {
    if(this.activeServer != null) {
      boolean isActive = false;
      boolean shouldStorageLoaded = false;
      lastTerminatedServer.start(isActive, shouldStorageLoaded, Collections.<Long>emptySet());
      this.activeServer.attachDownstreamPassive(lastTerminatedServer);
    } else {
      boolean isActive = true;
      boolean shouldStorageLoaded = true;
      Assert.assertTrue(null != this.mostRecentlyStoppedActiveServer);
      lastTerminatedServer.start(isActive, shouldStorageLoaded, this.mostRecentlyStoppedActiveServer.getSavedClientConnections());
      attachPassivesToActive(lastTerminatedServer);
      this.mostRecentlyStoppedActiveServer.connectSavedClientsTo(lastTerminatedServer);
      this.mostRecentlyStoppedActiveServer = null;

      PassthroughServer prevActive = PassthroughServerRegistry.getSharedInstance().registerServer(this.stripeName, lastTerminatedServer);
      Assert.assertTrue(prevActive == null);

      this.activeServer = lastTerminatedServer;
      this.notifyAll();
    }
  }

  private void internalTerminateActive() {
    Assert.assertTrue(this.activeServer != null);
    final PassthroughServer prevActiveServer = this.activeServer;
    this.activeServer = null;

    // disconnect clients and stop current active
    prevActiveServer.disconnectClients();
    prevActiveServer.stop();

    Assert.assertTrue(PassthroughServerRegistry.getSharedInstance().unregisterServer(this.stripeName) == prevActiveServer);

    // add current active to stopped servers list, so that election code won't consider this server
    this.stoppedPassthroughServers.add(prevActiveServer);

    final PassthroughServer electedActive = electActive();
    //electedActive could be null, when there is only one server in this cluster
    if(electedActive != null) {
      electedActive.promoteToActive();

      // attach all passives to new active and connect clients from old active
      attachPassivesToActive(electedActive);
      prevActiveServer.connectSavedClientsTo(electedActive);

      PassthroughServer prevActive = PassthroughServerRegistry.getSharedInstance().registerServer(this.stripeName, electedActive);
      Assert.assertTrue(prevActive == null);

      this.activeServer = electedActive;
      this.notifyAll();
    } else {
      // In this case, it means that no server in the stripe is running, at all.  In that case, we still need to hold onto this active server so that we can re-attach clients, whenever something is started.
      Assert.assertTrue(null == this.mostRecentlyStoppedActiveServer);
      this.mostRecentlyStoppedActiveServer = prevActiveServer;
    }
  }

  synchronized void terminateIfActive(PassthroughServer passthroughServer, RestartMode restartMode) throws PlatformStopException {
    if (activeServer == passthroughServer) {
      internalTerminateActive();
      startIfNeeded(passthroughServer, restartMode);
    } else {
      throw new PlatformStopException("Server is not in active state");
    }
  }

  synchronized void terminateIfPassive(PassthroughServer passthroughServer, RestartMode restartMode) throws PlatformStopException {
    if (activeServer != passthroughServer) {
      if (!this.stoppedPassthroughServers.contains(passthroughServer)) {
        internalTerminatePassive(passthroughServer);
        startIfNeeded(passthroughServer, restartMode);
      }
    } else {
      throw new PlatformStopException("Server is not in passive state");
    }
  }

  synchronized void terminate(PassthroughServer passthroughServer, RestartMode restartMode) {
    if (activeServer == passthroughServer) {
      internalTerminateActive();
    } else {
      if (!this.stoppedPassthroughServers.contains(passthroughServer)) {
        internalTerminatePassive(passthroughServer);
        startIfNeeded(passthroughServer, restartMode);
      }
    }
  }

  private synchronized void startIfNeeded(PassthroughServer passthroughServer, RestartMode restartMode) {
    if (restartMode == RestartMode.STOP_AND_RESTART) {
      this.stoppedPassthroughServers.remove(passthroughServer);
      startTerminatedServer(passthroughServer);
    }
  }
}
