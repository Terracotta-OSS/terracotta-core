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

import java.net.URI;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;


import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;


/**
 * TODO: Better rename this class as PassthroughStripeControl?
 * The implementation used to control a passthrough testing cluster which can contain multiple servers
 */
public class PassthroughClusterControl implements IClusterControl {
  private final String stripeName;
  private List<PassthroughServer> passthroughServers = new ArrayList<PassthroughServer>();
  private Set<PassthroughServer> stoppedPassthroughServers = new HashSet<PassthroughServer>();
  private Stack<PassthroughServer> terminatedServersInOrder = new Stack<PassthroughServer>();
  // elected active server
  private PassthroughServer activeServer;

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
  public synchronized void restartActive() throws Exception {
    Assert.assertTrue(this.activeServer != null);
    final PassthroughServer prevActiveServer = this.activeServer;
    this.activeServer = null;

    // disconnect clients and stop current active
    prevActiveServer.disconnectClients();
    prevActiveServer.stop();

    Assert.assertTrue(PassthroughServerRegistry.getSharedInstance().unregisterServer(this.stripeName) == prevActiveServer);

    // add current active to stopped servers list, so that election code won't consider this server
    this.stoppedPassthroughServers.add(prevActiveServer);

    // elect active
    PassthroughServer electedActive = electActive();

    // if newly elected active is null, just use previous active server as current active server
    // otherwise promote the elected active (was a passive) and start old active in passive mode
    if(electedActive == null) {
      electedActive = prevActiveServer;
      boolean isActive = true;
      boolean shouldStorageLoaded = true;
      prevActiveServer.start(isActive, shouldStorageLoaded);
    } else {
      electedActive.promoteToActive();
      boolean isActive = false;
      boolean shouldStorageLoaded = false;
      prevActiveServer.start(isActive, shouldStorageLoaded);
    }

    // previous active is alive now, remove it from stopped servers
    this.stoppedPassthroughServers.remove(prevActiveServer);

    // attach all passives to new active and connect clients from old active
    attachPassivesToActive(electedActive);
    prevActiveServer.connectSavedClientsTo(electedActive);

    PassthroughServer prevActive = PassthroughServerRegistry.getSharedInstance().registerServer(this.stripeName, electedActive);
    Assert.assertTrue(prevActive == null);

    this.activeServer = electedActive;
    this.notifyAll();
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
    this.terminatedServersInOrder.add(prevActiveServer);

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
    }
  }

  @Override
  public synchronized void startLastTerminatedServer() throws Exception {
    try {
      PassthroughServer lastTerminatedServer = this.terminatedServersInOrder.pop();
      if(this.activeServer != null) {
        boolean isActive = false;
        boolean shouldStorageLoaded = false;
        lastTerminatedServer.start(isActive, shouldStorageLoaded);
        this.activeServer.attachDownstreamPassive(lastTerminatedServer);
      } else {
        boolean isActive = true;
        boolean shouldStorageLoaded = false;
        lastTerminatedServer.start(isActive, shouldStorageLoaded);

        attachPassivesToActive(lastTerminatedServer);

        PassthroughServer prevActive = PassthroughServerRegistry.getSharedInstance().registerServer(this.stripeName, lastTerminatedServer);
        Assert.assertTrue(prevActive == null);

        this.activeServer = lastTerminatedServer;
        this.notifyAll();
      }
    } catch (EmptyStackException e) {
      throw new IllegalStateException("There are no terminated servers to start");
    }
  }


  @Override
  public synchronized void waitForActive() throws Exception {
    while (this.activeServer == null) {
      this.wait();
    }
  }

  @Override
  public synchronized void waitForPassive() throws Exception {
    // we sync passives during the active election itself, so just wait on same state
    while (this.activeServer == null) {
      this.wait();
    }
  }

  @Override
  public Connection createConnectionToActive() {
    URI uri = URI.create("passthrough://" + this.stripeName);
    Connection connection = null;
    try {
      connection = ConnectionFactory.connect(uri, null);
    } catch (ConnectionException e) {
      Assert.unexpected(e);
    }
    return connection;
  }

  @Override
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

}
