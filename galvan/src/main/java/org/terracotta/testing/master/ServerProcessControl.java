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
package org.terracotta.testing.master;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;


/**
 * This component implements the IMultiProcessControl interface as a heavily-synchronized object in order to act as
 * a choke-point such that calls to interact with the test state are forced to be sequential even though the
 * underlying logic is asynchronous.  By doing the wait() calls, in a different object, while called through here,
 * we block other attempts to change test state.  This protects us from complex situations such as 2 clients trying
 * to restart the same ServerProcess object, because they both reached for the active at the same time, for example.
 * 
 * Hence, all the public methods are synchronized, even though we don't actually wait/notify here.
 */
public class ServerProcessControl implements IMultiProcessControl {
  private final StateInterlock stateInterlock;
  private final ContextualLogger logger;

  public ServerProcessControl(StateInterlock stateInterlock, ContextualLogger logger) {
    this.stateInterlock = stateInterlock;
    this.logger = logger;
  }

  @Override
  public synchronized void synchronizeClient() {
    this.logger.output(">>> synchronizeClient");
    // Do nothing - this is just for demonstration purposes.
    this.logger.output("<<< synchronizeClient");
  }

  @Override
  public synchronized void terminateActive() throws GalvanFailureException {
    this.logger.output(">>> terminateActive");
    
    // Get the active and stop it.
    IGalvanServer active = this.stateInterlock.getActiveServer();
    // We expect that the test knows there is an active (might change in the future).
    if (null == active) {
      throw new IllegalStateException("No server in active state");
    }
    safeStop(active);
    
    this.logger.output("<<< terminateActive");
  }

  @Override
  public synchronized void terminateOnePassive() throws GalvanFailureException {
    this.logger.output(">>> terminateOnePassive");
    
    // Pick an arbitrary passive.
    IGalvanServer onePassive = this.stateInterlock.getOnePassiveServer();
    // It is acceptable to call this in the case where there is no passive.  That is a "do nothing" situation.
    if (null != onePassive) {
      // Stop the server
      safeStop(onePassive);
    }
    
    this.logger.output("<<< terminateOnePassive");
  }

  @Override
  public synchronized void terminateOneDiagnostic() throws GalvanFailureException {
    this.logger.output(">>> terminateOneDiagnostic");

    // Pick an arbitrary server.
    IGalvanServer oneDiagnosticServer = this.stateInterlock.getOneDiagnosticServer();
    // It is acceptable to call this in the case where there is no diagnostic server. That is a "do nothing" situation.
    if (null != oneDiagnosticServer) {
      // Stop the server
      safeStop(oneDiagnosticServer);
    }

    this.logger.output("<<< terminateOneDiagnostic");
  }

  @Override
  public synchronized void startOneServer() throws GalvanFailureException {
    this.logger.output(">>> startOneServer " + this.stateInterlock);
    startServer();
    this.logger.output("<<< startOneServer");
  }

  public synchronized void startServer() throws GalvanFailureException {
    IGalvanServer server = this.stateInterlock.getOneTerminatedServer();
    if (null == server) {
      throw new IllegalStateException("Tried to start one server when none are terminated");
    }
    safeStart(server);
  }

  @Override
  public synchronized void startAllServers() throws GalvanFailureException {
    this.logger.output(">>> startAllServers " + this.stateInterlock);
    startServers();
    this.logger.output("<<< startAllServers " + this.stateInterlock);
  }

  private void startServers() throws GalvanFailureException {
    IGalvanServer server = this.stateInterlock.getOneTerminatedServer();
    List<IGalvanServer> started = new LinkedList<>();
    while (null != server) {
      safeStart(server);
      started.add(server);
      // Wait for it to start up (since we need to grab a different one in the next call).
      server = this.stateInterlock.getOneTerminatedServer();
    }
    started.forEach(IGalvanServer::waitForRunning);
  }

  @Override
  public synchronized void terminateAllServers() throws GalvanFailureException {
    this.logger.output(">>> terminateAllServers " + this.stateInterlock);
    // Wait until all servers are in a reasonable state.
    this.stateInterlock.waitForAllServerRunning();
    
    // NOTE:  We want to get the passives, first, to avoid active fail-over causing us not to know the state of a server when looking for it.
    // Get all the passives.
    this.stateInterlock.collectAllRunningServers().forEach(s->{
      safeStop(s);
    });

    this.logger.output("<<< terminateAllServers");
  }

  @Override
  public synchronized void waitForActive() throws GalvanFailureException {
    this.logger.output(">>> waitForActive");
    
    this.stateInterlock.waitForActiveServer();
    
    this.logger.output("<<< waitForActive");
  }

  @Override
  public synchronized void waitForRunningPassivesInStandby() throws GalvanFailureException {
    this.logger.output(">>> waitForRunningPassivesInStandby " + this.stateInterlock);
    
    this.stateInterlock.waitForAllServerReady();
    
    this.logger.output("<<< waitForRunningPassivesInStandby " + this.stateInterlock);
  }


  private void safeStart(IGalvanServer server) {
    try {
      server.start();
    } catch (IOException e) {
      // Unexpected, given that this server was already started, at one point.
      Assert.unexpected(e);
    }
  }

  private void safeStop(IGalvanServer server) {
    try {
    long start = System.currentTimeMillis();
    this.logger.output(">>> stoppingServer " + server.toString());
      server.stop();
      server.waitForTermination();
    this.logger.output("<<< stoppingServer " + server.toString() + " " + (System.currentTimeMillis() - start) + "ms");
    } catch (InterruptedException e) {
      // Interruption not expected in these tests.
      Assert.unexpected(e);
    }
  }
}
