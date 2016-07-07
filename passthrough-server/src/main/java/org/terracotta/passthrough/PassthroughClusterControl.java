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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.ArrayList;
import java.util.List;


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
    Assert.assertTrue(passthroughServer != null);
    this.passthroughServers.add(passthroughServer);
    for (PassthroughServer ps : passthroughServers) {
      Assert.assertTrue(ps != null);
      this.passthroughServers.add(ps);
    }
    bootstrapCluster();
  }

  @Override
  public synchronized void waitForActive() throws Exception {
    while (this.activeServer == null) {
      this.wait();
    }
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
      // Disconnect it from the active.
      if (null != this.activeServer) {
        this.activeServer.detachDownstreamPassive(victim);
      }
      // Stop the server.
      victim.stop();
      // Add it to our stopped list so we don't consider it for election.
      this.stoppedPassthroughServers.add(victim);
    }
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

  public void tearDown() {
    for (PassthroughServer passthroughServer : passthroughServers) {
      // don't stop twice
      if(!stoppedPassthroughServers.contains(passthroughServer)) {
        passthroughServer.stop();
      }
    }
    PassthroughServer removedServer = PassthroughServerRegistry.getSharedInstance().unregisterServer(this.stripeName);
    Assert.assertTrue(this.activeServer == removedServer);
  }

  private synchronized void bootstrapCluster() {
    Assert.assertTrue(this.activeServer == null);

    final PassthroughServer electedActive = electActive();

    boolean isActive = true;
    boolean shouldStorageLoaded = false;

    electedActive.start(isActive, shouldStorageLoaded);
    for (PassthroughServer passthroughServer : passthroughServers) {
      if(!electedActive.equals(passthroughServer)) {
        passthroughServer.start(!isActive, shouldStorageLoaded);
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
      lastTerminatedServer.start(isActive, shouldStorageLoaded);
      this.activeServer.attachDownstreamPassive(lastTerminatedServer);
    } else {
      boolean isActive = true;
      boolean shouldStorageLoaded = true;
      lastTerminatedServer.start(isActive, shouldStorageLoaded);

      attachPassivesToActive(lastTerminatedServer);
      Assert.assertTrue(null != this.mostRecentlyStoppedActiveServer);
      this.mostRecentlyStoppedActiveServer.connectSavedClientsTo(lastTerminatedServer);
      this.mostRecentlyStoppedActiveServer = null;

      PassthroughServer prevActive = PassthroughServerRegistry.getSharedInstance().registerServer(this.stripeName, lastTerminatedServer);
      Assert.assertTrue(prevActive == null);

      this.activeServer = lastTerminatedServer;
      this.notifyAll();
    }
  }
}
