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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;


/**
 * Handles the description, installation, and start-up of a stripe of servers in a cluster.
 */
public class StripeInstaller {
  private final GalvanStateInterlock interlock;
  private final ITestStateManager stateManager;
  private final VerboseManager stripeVerboseManager;
  private final String stripeInstallDirectory;
  private final String kitOriginDirectory;
  private final List<String> extraJarPaths;
  private final List<ServerInstallation> installedServers;
  private boolean isBuilt;
  
  public StripeInstaller(GalvanStateInterlock interlock, ITestStateManager stateManager, VerboseManager stripeVerboseManager, String stripeInstallDirectory, String kitOriginDirectory, List<String> extraJarPaths) {
    this.interlock = interlock;
    this.stateManager = stateManager;
    this.stripeVerboseManager = stripeVerboseManager;
    this.stripeInstallDirectory = stripeInstallDirectory;
    this.kitOriginDirectory = kitOriginDirectory;
    this.extraJarPaths = extraJarPaths;
    
    this.installedServers = new Vector<ServerInstallation>();
  }
  
  public void installNewServer(String serverName, int heapInM, int debugPort) throws IOException {
    // Our implementation installs all servers before starting any (just an internal consistency check).
    Assert.assertFalse(this.isBuilt);
    // Create the logger for the intallation.
    ContextualLogger fileHelperLogger = this.stripeVerboseManager.createFileHelpersLogger();
    // Create a copy of the server for this installation.
    String installPath = FileHelpers.createTempCopyOfDirectory(fileHelperLogger, this.stripeInstallDirectory, serverName, this.kitOriginDirectory);
    // Copy the extra jars this test needs into the new installation.
    FileHelpers.copyJarsToServer(fileHelperLogger, installPath, this.extraJarPaths);

    //Copy a cutom logback configuration
    Path serverPath = FileSystems.getDefault().getPath(installPath, "server", "lib", "logback-test.xml");
    Files.copy(this.getClass().getResourceAsStream("/logback-test.xml"), serverPath);

    // Create the object representing this single installation and add it to the list for this stripe.
    ServerInstallation installation = new ServerInstallation(this.interlock, this.stateManager, this.stripeVerboseManager, serverName, new File(installPath), heapInM, debugPort);
    this.installedServers.add(installation);
  }
  
  public void installConfig(String configText) throws IOException {
    Assert.assertFalse(this.isBuilt);
    for (ServerInstallation installation : this.installedServers) {
      installation.overwriteConfig(configText);
    }
  }
  
  public void startServers() {
    Assert.assertFalse(this.isBuilt);
    for (ServerInstallation installation : this.installedServers) {
      // Note that starting the process will put it into the interlock and the server will notify it of state changes.
      ServerProcess newProcess = installation.createNewProcess();
      try {
        newProcess.start();
      } catch (FileNotFoundException e) {
        Assert.unexpected(e);
      }
    }
    this.isBuilt = true;
  }
}
