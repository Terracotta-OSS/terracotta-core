/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.testing.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;


/**
 * This represents the state of the running galvan test cluster.  This is where threads wait for a client or server
 *  to enter a specific state.
 * NOTE:  This is only where changes are noted, now where the changes to sub-processes actually happen.
 * 
 * NOTE ABOUT LOCKING:  In order to ensure that there is a single wait/notify monitor for the entire testing system
 *  (keeps some things simpler and allows easier extensibility), the ITestWaiter given to the constructor will be
 *  used as the wait/notify monitor.
 */
public class StateInterlock implements IGalvanStateInterlock {
  private final ContextualLogger logger;
  private final ITestWaiter sharedLockState;
  private boolean isShuttingDown;
  private boolean ignoreServerCrashes;

  private final Deque<IGalvanServer> servers = new ConcurrentLinkedDeque<>();

  // ----- CLIENT STATE -----
  // When a client starts up, we register it as a running client and wait for it to report that it terminated.
  private final Deque<ClientRunner> runningClients = new ConcurrentLinkedDeque<>();


  public StateInterlock(ContextualLogger logger, ITestWaiter sharedLockState) {
    this.logger = logger;
    this.sharedLockState = sharedLockState;
  }

  public void ignoreServerCrashes(boolean set) {
    ignoreServerCrashes = set;
    servers.forEach(process->process.setCrashExpected(ignoreServerCrashes));
  }

  // ----- REGISTRATION -----
  public void registerNewServer(IGalvanServer newServer) {
    newServer.setCrashExpected(ignoreServerCrashes);
    this.logger.output("registerNewServer: " + newServer);
    // No new registration during shutdown.
    Assert.assertFalse(this.isShuttingDown);
    Assert.assertFalse(this.servers.contains(newServer));
    this.servers.add(newServer);
  }

  public void registerRunningClient(ClientRunner runningClient) throws GalvanFailureException {
    this.logger.output("registerRunningClient: " + runningClient);
    // No new registration during shutdown.
    if (this.isShuttingDown) {
      throw new GalvanFailureException("Failed to register new client when already shutting down");
    }
    Assert.assertFalse(this.isShuttingDown);
    Assert.assertFalse(this.runningClients.contains(runningClient));
    this.runningClients.add(runningClient);
  }


  // ----- WAITING-----
  public synchronized void waitForClientTermination() throws GalvanFailureException {
    this.logger.output("> waitForClientTermination");
    while (!this.sharedLockState.checkDidPass() && !this.runningClients.isEmpty()) {
      safeWait();
    }
    this.logger.output("< waitForClientTermination");
  }

  public synchronized void waitForActiveServer() throws GalvanFailureException {
    this.logger.output("> waitForActiveServer");
    IGalvanServer active = getActiveServer();
    while (!this.sharedLockState.checkDidPass() &&  active == null) {
      safeWait();
      active = getActiveServer();
    }
    this.logger.output("< waitForActiveServer active:" + active);
  }

  public void waitForAllServerRunning() throws GalvanFailureException {
    this.logger.output("> waitForAllServerRunning");
    this.servers.forEach(server->server.waitForRunning());
    this.logger.output("< waitForAllServerRunning");
  }

  public void waitForAllServerReady() throws GalvanFailureException {
    this.logger.output("> waitForAllServerReady");
    this.servers.forEach(server->server.waitForReady());
    this.logger.output("< waitForAllServerReady");
  }
  
  private void safeWait() {
    try {
      wait();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    }
  }

  // ----- CHECK STATE-----
  public IGalvanServer getActiveServer() throws GalvanFailureException {
    IGalvanServer process = this.servers.stream().filter(IGalvanServer::isActive).findAny().orElse(null);
    if (process != null) {
      this.logger.output("getActiveServer " + process);
    }
      this.sharedLockState.checkDidPass();
      return process;
  }

  public IGalvanServer getOnePassiveServer() throws GalvanFailureException {
      this.logger.output("getOnePassiveServer");
      this.sharedLockState.checkDidPass();
      return this.servers.stream().filter(s->s.getCurrentState() == ServerMode.PASSIVE).findAny().orElse(null);
  }

  public IGalvanServer getOneDiagnosticServer() throws GalvanFailureException {
      this.logger.output("getOneDiagnosticServer");
      this.sharedLockState.checkDidPass();
      return this.servers.stream().filter(s->s.getCurrentState() == ServerMode.DIAGNOSTIC).findAny().orElse(null);
  }

  public IGalvanServer getOneTerminatedServer() throws GalvanFailureException {
      this.logger.output("getOneTerminatedServer");
      this.sharedLockState.checkDidPass();
      if (!isShuttingDown) {
        return this.servers.stream().filter(s->s.getCurrentState() == ServerMode.TERMINATED).findAny().orElse(null);
      }
      return null;
  }

  boolean checkDidPass() throws GalvanFailureException {
    return this.sharedLockState.checkDidPass();
  }

  // ----- CHANGE STATE-----
  public synchronized void serverBecameActive(InlineServerProcess server, ServerMode previous) {
      this.logger.output("serverBecameActive: " + server + " was passive:" + (previous == ServerMode.PASSIVE));
      notifyAll();
  }

  public synchronized void clientDidTerminate(ClientRunner client) {
      this.logger.output("clientDidTerminate: " + client);
      boolean didRemove = this.runningClients.remove(client);
      Assert.assertTrue(didRemove);
      notifyAll();
  }
  
  Collection<IGalvanServer> collectAllRunningServers() {
    return this.servers.stream().filter(s->s.getCurrentState() != ServerMode.TERMINATED)
        .sorted((a,b)->a.getCurrentState().ordinal() - b.getCurrentState().ordinal()).collect(Collectors.toList());
  }
  
  private Collection<ClientRunner> collectRunningClients() {
      List<ClientRunner> copy = new ArrayList<>();
      copy.addAll(this.runningClients);
      return copy;
  }
  
  private boolean checkIfEmpty() {
    return this.collectAllRunningServers().isEmpty()
        && this.runningClients.isEmpty();
  }
  // ----- CLEANUP-----
  public void forceShutdown() throws GalvanFailureException {
      this.logger.output("> forceShutdown");
      // Set the flag that we are shutting down.  That way, any servers which were concurrently coming online can be stopped when they check in.
      this.isShuttingDown = true;

//  trying to debug a Galvan hang where not all the servers are seen
      long timeExpired = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2);
      // We wait until there is no active, no passives, no unknown servers, and no running clients.
      while (timeExpired > System.currentTimeMillis() && !checkIfEmpty()) {
        this.logger.output("* forceShutdown waiting on active: " + (getActiveServer() == null)
            + " servers: " + this.servers.stream().map(IGalvanServer::getCurrentState).map(ServerMode::toString).collect(Collectors.joining(","))
            + " clients: " + this.runningClients.size()
            );
        for (ClientRunner client : collectRunningClients()) {
          client.forceTerminate();
        }

        collectAllRunningServers().forEach(this::safeStop);
      }
      if (System.currentTimeMillis() > timeExpired) {
        this.logger.output("* forceShutdown FAILED waiting on active: " + (getActiveServer() == null)
            + " servers: " + this.servers.stream().map(IGalvanServer::getCurrentState).map(ServerMode::toString).collect(Collectors.joining(","))
            + " clients: " + this.runningClients.size());
        throw new RuntimeException("FORCE SHUTDOWN FAILED:" + toString());
      }
      
      this.logger.output("< forceShutdown");
  }

  private void safeStop(IGalvanServer server) {
    try {
      long start = System.currentTimeMillis();
      this.logger.output("< safeStop " + server.toString());
      this.logger.output("Stopping " + server);
      server.stop();
      server.waitForTermination();
      this.logger.output("> safeStop " + server.toString() + " " + (System.currentTimeMillis() - start) + "ms");
    } catch (InterruptedException e) {
      // Not expected in this usage - we are shutting down.
      Assert.unexpected(e);
    }
  }


  @Override
  public String toString() {
    return super.toString()
        + "\n\tServers: " + this.servers
        + "\n\tClients: " + this.runningClients
        ;
  }
}