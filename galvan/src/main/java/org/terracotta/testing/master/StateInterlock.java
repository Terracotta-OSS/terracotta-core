/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

  public synchronized void waitForClientTermination() throws GalvanFailureException {
    waitForClientTermination(0);
  }

  // ----- WAITING-----
  public synchronized void waitForClientTermination(long timeout) throws GalvanFailureException {
    this.logger.output("> waitForClientTermination");
    while (!this.sharedLockState.checkDidPass() && !this.runningClients.isEmpty()) {
      try {
        wait(timeout);
      } catch (InterruptedException ie) {
        throw new GalvanFailureException("interrupted", ie);
      }
    }
    this.logger.output("< waitForClientTermination");
  }

  public synchronized void waitForActiveServer() throws GalvanFailureException {
    this.logger.output("> waitForActiveServer " + this.servers);
    IGalvanServer active = getActiveServer();
    while (!this.sharedLockState.checkDidPass() &&  active == null) {
      safeWait();
      active = getActiveServer();
    }
    this.logger.output("< waitForActiveServer active:" + active);
  }

  public void waitForAllServerRunning() throws GalvanFailureException {
    this.logger.output("> waitForAllServerRunning " + this.servers);
    this.servers.forEach(server->server.waitForRunning());
    this.logger.output("< waitForAllServerRunning " + this.servers);
  }

  public void waitForAllServerReady() throws GalvanFailureException {
    this.logger.output("> waitForAllServerReady " + this.servers);
    this.servers.forEach(server->server.waitForReady());
    this.logger.output("< waitForAllServerReady " + this.servers);
  }
  
  public void waitForAllServerTerminated() throws GalvanFailureException {
    this.logger.output("> waitForAllServerTerminated " + this.servers);
    this.servers.forEach(server->server.waitForTermination());
    this.logger.output("< waitForAllServerTerminated " + this.servers);
  }

  public void waitForAllServerTerminated(long perServerTimeLimit) throws GalvanFailureException {
    this.logger.output("> waitForAllServerTerminated " + this.servers);
    this.servers.forEach(server->server.waitForTermination(perServerTimeLimit));
    this.logger.output("< waitForAllServerTerminated " + this.servers);
  }

  private void safeWait() throws GalvanFailureException {
    try {
      wait();
    } catch (InterruptedException e) {
      throw new GalvanFailureException("interrupted", e);
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
      IGalvanServer one = null;
      if (!isShuttingDown) {
        one = this.servers.stream().filter(s->s.getCurrentState() == ServerMode.TERMINATED).findAny().orElse(null);
      }
      this.logger.output("getOneTerminatedServer " + one);
      if (one != null) {
        IGalvanServer newOne = one.newInstance();
        if (newOne != one) {
          this.servers.remove(one);
          one = newOne;
        }
      }
      return one;
  }

  boolean checkDidPass() throws GalvanFailureException {
    return this.sharedLockState.checkDidPass();
  }

  // ----- CHANGE STATE-----
  public synchronized void serverBecameActive(IGalvanServer server, ServerMode previous) {
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
  @Override
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
        collectAllRunningServers().forEach(this::safeWaitForTermination);
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
      this.logger.output("Stopping " + server);
      server.stop();
    } catch (InterruptedException e) {
      // Not expected in this usage - we are shutting down.
      Assert.unexpected(e);
    }
  }

  private void safeWaitForTermination(IGalvanServer server) {
    long start = System.currentTimeMillis();
    this.logger.output("< waiting for termination " + server.toString());
    server.waitForTermination(5000);
    this.logger.output("> waiting for termination " + server.toString() + " " + (System.currentTimeMillis() - start) + "ms");
  }

  @Override
  public String toString() {
    return super.toString()
        + "\n\tServers: " + this.servers
        + "\n\tClients: " + this.runningClients
        ;
  }
}