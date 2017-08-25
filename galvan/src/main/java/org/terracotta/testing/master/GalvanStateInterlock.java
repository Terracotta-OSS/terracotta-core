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

import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

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
public class GalvanStateInterlock implements IGalvanStateInterlock {
  private final ContextualLogger logger;
  private final ITestWaiter sharedLockState;
  private boolean isShuttingDown;
  private boolean ignoreServerCrashes;


  // ----- SERVER STATE -----
  // There can be one active unless we are still waiting for a server to enter an active state.
  private ServerProcess activeServer;
  // There can be any number of passives (a server enters this state when it becomes PASSIVE_STANDBY).
  private final List<ServerProcess> passiveServers = new Vector<ServerProcess>();
  // A server which is running but hasn't yet reported in its state waits as "unknownRunning".
  private final List<ServerProcess> unknownRunningServers = new Vector<ServerProcess>();
  // A server which has returned an exit status, or hasn't yet become running, is a terminated server.
  private final List<ServerProcess> terminatedServers = new Vector<ServerProcess>();


  // ----- CLIENT STATE -----
  // When a client starts up, we register it as a running client and wait for it to report that it terminated.
  private final List<ClientRunner> runningClients = new Vector<ClientRunner>();


  public GalvanStateInterlock(ContextualLogger logger, ITestWaiter sharedLockState) {
    this.logger = logger;
    this.sharedLockState = sharedLockState;
  }

  public void ignoreServerCrashes(boolean set) {
    synchronized (this.sharedLockState) {
      ignoreServerCrashes = set;
      if (activeServer != null) {
        activeServer.setCrashExpected(ignoreServerCrashes);
      }
      passiveServers.forEach(process->process.setCrashExpected(ignoreServerCrashes));
      unknownRunningServers.forEach(process->process.setCrashExpected(ignoreServerCrashes));
      terminatedServers.forEach(process->process.setCrashExpected(ignoreServerCrashes));
    }
  }

  // ----- REGISTRATION -----
  @Override
  public void registerNewServer(ServerProcess newServer) {
    synchronized (this.sharedLockState) {
      newServer.setCrashExpected(ignoreServerCrashes);
      this.logger.output("registerNewServer: " + newServer);
      // No new registration during shutdown.
      Assert.assertFalse(this.isShuttingDown);
      Assert.assertFalse(this.terminatedServers.contains(newServer));
      this.terminatedServers.add(newServer);
    }
  }

  @Override
  public void registerRunningClient(ClientRunner runningClient) throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("registerRunningClient: " + runningClient);
      // No new registration during shutdown.
      if (this.isShuttingDown) {
        throw new GalvanFailureException("Failed to register new client when already shutting down");
      }
      Assert.assertFalse(this.isShuttingDown);
      Assert.assertFalse(this.runningClients.contains(runningClient));
      this.runningClients.add(runningClient);
    }
  }


  // ----- WAITING-----
  @Override
  public void waitForClientTermination() throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("> waitForClientTermination");
      while (!this.sharedLockState.checkDidPass() && !this.runningClients.isEmpty()) {
        safeWait();
      }
      this.logger.output("< waitForClientTermination");
    }
  }

  @Override
  public void waitForActiveServer() throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("> waitForActiveServer");
      while (!this.sharedLockState.checkDidPass() && (null == this.activeServer)) {
        safeWait();
      }
      this.logger.output("< waitForActiveServer " + this.activeServer);
    }
  }

  @Override
  public void waitForServerRunning(ServerProcess startingUpServer) throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("> waitForServerRunning: " + startingUpServer);
      // Wait until it is no longer terminated (whether it is unknown, active, or passive).
      while (!this.sharedLockState.checkDidPass() && (this.terminatedServers.contains(startingUpServer))) {
        safeWait();
      }
      this.logger.output("< waitForServerRunning: " + startingUpServer);
    }
  }

  @Override
  public void waitForServerTermination(ServerProcess terminatedServer) throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("> waitForServerTermination: " + terminatedServer);
      while (!this.sharedLockState.checkDidPass() && !this.terminatedServers.contains(terminatedServer)) {
        safeWait();
      }
      this.logger.output("< waitForServerTermination: " + terminatedServer);
    }
  }

  @Override
  public void waitForAllServerRunning() throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("> waitForAllServerRunning");
      while (!this.sharedLockState.checkDidPass() && (!this.terminatedServers.isEmpty())) {
        safeWait();
      }
      this.logger.output("< waitForAllServerRunning");
    }
  }

  @Override
  public void waitForAllServerReady() throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("> waitForAllServerReady");
      while (!this.sharedLockState.checkDidPass() && !this.unknownRunningServers.isEmpty()) {
        safeWait();
      }
      this.logger.output("< waitForAllServerReady");
    }
  }

  private void safeWaitWithTimeout(long time, TimeUnit units) {
    try {
      this.sharedLockState.wait(units.toMillis(time));
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    }
  }
  
  private void safeWait() {
    try {
      this.sharedLockState.wait();
    } catch (InterruptedException e) {
      Assert.unexpected(e);
    }
  }


  // ----- CHECK STATE-----
  @Override
  public ServerProcess getActiveServer() throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("getActiveServer " + this.activeServer);
      this.sharedLockState.checkDidPass();
      return this.activeServer;
    }
  }

  @Override
  public ServerProcess getOnePassiveServer() throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("getOnePassiveServer");
      this.sharedLockState.checkDidPass();
      ServerProcess one = null;
      if (!this.passiveServers.isEmpty()) {
        one = this.passiveServers.get(0);
      }
      return one;
    }
  }

  @Override
  public ServerProcess getOneTerminatedServer() throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("getOneTerminatedServer");
      this.sharedLockState.checkDidPass();
      ServerProcess one = null;
      if (!this.terminatedServers.isEmpty()) {
        one = this.terminatedServers.get(0);
      }
      return one;
    }
  }


  // ----- CHANGE STATE-----
  @Override
  public void serverBecameActive(ServerProcess server) {
    synchronized (this.sharedLockState) {
      localAssert(null == this.activeServer, server);
      boolean didRemove = this.unknownRunningServers.remove(server);
      boolean wasPassive = false;
      if (!didRemove) {
        didRemove = this.passiveServers.remove(server);
        wasPassive = true;
      }
      this.logger.output("serverBecameActive: " + server + " was passive:" + wasPassive);
      localAssert(didRemove, server);
      this.activeServer = server;
      this.sharedLockState.notifyAll();
    }
  }
  

  public boolean isServerRunning(ServerProcess server) {
    synchronized (this.sharedLockState) {
      return this.unknownRunningServers.contains(server) || this.passiveServers.contains(server) || server.equals(this.activeServer);
    }
  }  

  @Override
  public void serverBecamePassive(ServerProcess server) {
    synchronized (this.sharedLockState) {
      boolean didRemove = this.unknownRunningServers.remove(server);
      this.logger.output("serverBecamePassive: " + server);
      localAssert(didRemove, server);
      this.passiveServers.add(server);
      this.sharedLockState.notifyAll();
    }
  }

  @Override
  public void serverDidShutdown(ServerProcess server) {
    synchronized (this.sharedLockState) {
      this.logger.output("serverDidShutdown: " + server);
      // We need to see which kind of server this is (it must be something which wasn't already terminated).
      if (server == this.activeServer) {
        this.activeServer = null;
      } else if (this.passiveServers.contains(server)) {
        this.passiveServers.remove(server);
      } else if (this.unknownRunningServers.contains(server)) {
        this.unknownRunningServers.remove(server);
      } else {
        // We failed to find the server somewhere, which is a bug.
        Assert.assertFalse(true);
      }
      this.terminatedServers.add(server);
      this.sharedLockState.notifyAll();
    }
  }

  @Override
  public void serverDidStartup(ServerProcess server) {
    synchronized (this.sharedLockState) {
      server.setCrashExpected(ignoreServerCrashes);
      this.logger.output("serverDidStartup: " + server);
      boolean didRemove = this.terminatedServers.remove(server);
      localAssert(didRemove, server);
      this.unknownRunningServers.add(server);
      // Check if this server is a late arrival - explicit termination, in that case.
      if (this.isShuttingDown) {
        this.logger.output("explicit stop of late arrival: " + server);
        safeStop(server);
      }
      this.sharedLockState.notifyAll();
    }
  }

  @Override
  public void serverWasZapped(ServerProcess server) {
    synchronized (this.sharedLockState) {
      this.logger.output("serverWasZapped: " + server);
      // Note that a server can be zapped at pretty well any point.
      // Normally, this happens only when the server isn't yet in a known state (unknownRunningServers) but network
      //  interruptions can cause this to happen at other point, once the partition is healed.  A passive can be zapped
      //  because it is out of sync with its active.  An active can be zapped due to a split-brain.
      boolean didRemove = this.unknownRunningServers.remove(server);
      if (!didRemove) {
        // Try the passives.
        didRemove = this.passiveServers.remove(server);
      }
      if (!didRemove) {
        // Try the active.
        if (this.activeServer == server) {
          this.activeServer = null;
          didRemove = true;
        }
      }
      localAssert(didRemove, server);
      this.unknownRunningServers.add(server);
      this.sharedLockState.notifyAll();
    }
  }

  @Override
  public void clientDidTerminate(ClientRunner client) {
    synchronized (this.sharedLockState) {
      this.logger.output("clientDidTerminate: " + client);
      boolean didRemove = this.runningClients.remove(client);
      Assert.assertTrue(didRemove);
      this.sharedLockState.notifyAll();
    }
  }


  // ----- CLEANUP-----
  @Override
  public void forceShutdown() throws GalvanFailureException {
    synchronized (this.sharedLockState) {
      this.logger.output("> forceShutdown");
      // Set the flag that we are shutting down.  That way, any servers which were concurrently coming online can be stopped when they check in.
      this.isShuttingDown = true;
//  trying to debug a Galvan hang where not all the servers are seen
      long timeExpired = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
      // We wait until there is no active, no passives, no unknown servers, and no running clients.
      while (timeExpired > System.currentTimeMillis() && (
          (null != this.activeServer)
          || !this.passiveServers.isEmpty()
          || !this.unknownRunningServers.isEmpty()
          || !this.runningClients.isEmpty()
        )
      ) {
        this.logger.output("* forceShutdown waiting on active: " + (null != this.activeServer)
            + " passives: " + this.passiveServers.size()
            + " unknown: " + this.unknownRunningServers.size()
            + " clients: " + this.runningClients.size()
            );
        // Force shut-down all clients, all passives, and the active.
        // (note that we won't modify the collections here, just walk them - we are synchronized)
        // this is inside the loop because servers can transition from terminated to active or passive 
        // when the lock is released during the wait, if access is at all fair, should not cause double
        // shutowns
        for (ClientRunner client : this.runningClients) {
          client.forceTerminate();
        }
        for (ServerProcess server : this.unknownRunningServers) {
          safeStop(server);
        }
        for (ServerProcess server : this.passiveServers) {
          safeStop(server);
        }
        if (null != this.activeServer) {
          safeStop(this.activeServer);
        }
        safeWaitWithTimeout(10, TimeUnit.SECONDS);
      }
      if (System.currentTimeMillis() > timeExpired) {
        this.logger.output("* forceShutdown FAILED waiting on active: " + (null != this.activeServer)
            + " passives: " + this.passiveServers.size()
            + " unknown: " + this.unknownRunningServers.size()
            + " clients: " + this.runningClients.size());
        throw new RuntimeException("FORCE SHUTDOWN FAILED:" + toString());
      }
      
      this.logger.output("< forceShutdown");
    }
  }

  private void safeStop(ServerProcess server) {
    try {
      this.logger.output("Stopping " + server);
      server.stop();
    } catch (InterruptedException e) {
      // Not expected in this usage - we are shutting down.
      Assert.unexpected(e);
    }
  }


  @Override
  public String toString() {
    return super.toString()
        + "\n\tActive: " + this.activeServer
        + "\n\tPassives: " + this.passiveServers
        + "\n\tUnknown: " + this.unknownRunningServers
        + "\n\tTerminated: " + this.terminatedServers
        + "\n\tClients: " + this.runningClients
        ;
  }

  /**
   * A diagnostic helper function which is likely only temporary.
   * Increases the amount of data output when a failing assertion is triggered, directly to STDERR.
   * This is done since the outstanding Galvan bugs appear to only be intermittent and happen at high concurrency levels so
   *  this allows us to get a better snapshot of what happened.
   * 
   * @param clause The assertion value (output triggers on false).
   * @param server The server we were trying to add/remove/change at the time.
   */
  private void localAssert(boolean clause, ServerProcess server) {
    if (!clause) {
      System.err.println("FAIL USING " + server);
      System.err.println(toString());
    }
    Assert.assertTrue(clause);
  }
}