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

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.config.StripeConfiguration;
import org.terracotta.testing.logging.VerboseManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


/**
 * Handles the description, installation, and start-up of a stripe of servers in a cluster.
 */
public class StripeInstaller {
  private final StateInterlock interlock;
  private final ITestStateManager stateManager;
  private final VerboseManager stripeVerboseManager;
  private final StripeConfiguration stripeConfig;
  private final List<ServerProcess> serverProcesses = new ArrayList<>();
  private boolean isBuilt;

  public StripeInstaller(StateInterlock interlock, ITestStateManager stateManager, VerboseManager stripeVerboseManager, StripeConfiguration stripeConfig) {
    this.interlock = interlock;
    this.stateManager = stateManager;
    this.stripeVerboseManager = stripeVerboseManager;
    this.stripeConfig = stripeConfig;
  }

  public void installNewServer(String serverName, Path workingDir, int debugPort, Supplier<String[]> startupCommandSupplier) throws IOException {
    // Our implementation installs all servers before starting any (just an internal consistency check).
    Assert.assertFalse(this.isBuilt);
    // server install is now at the stripe level to use less disk
    Files.createDirectories(workingDir);
    // Create the object representing this single installation and add it to the list for this stripe.
    VerboseManager serverVerboseManager = this.stripeVerboseManager.createComponentManager("[" + serverName + "]");
    ServerProcess serverProcess = new ServerProcess(this.interlock, this.stateManager, serverVerboseManager, serverName,
        workingDir, stripeConfig.getServerHeapInM(), debugPort, stripeConfig.getServerProperties(), startupCommandSupplier);
    serverProcesses.add(serverProcess);
  }

  public void startServers() {
    Assert.assertFalse(this.isBuilt);
    for (ServerProcess newProcess : serverProcesses) {
      try {
        // Note that starting the process will put it into the interlock and the server will notify it of state changes.
        newProcess.start();
      } catch (IOException e) {
        Assert.unexpected(e);
      }
    }
    this.isBuilt = true;
  }
}
