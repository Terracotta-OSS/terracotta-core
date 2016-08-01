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
import java.io.FileOutputStream;
import java.io.IOException;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.VerboseManager;


/**
 * The physical representation of a server installation, on disk.  Server processes can be started from the installation.
 */
public class ServerInstallation {
  private final GalvanStateInterlock stateInterlock;
  private final ITestStateManager stateManager;
  private final VerboseManager stripeVerboseManager;
  private final String serverName;
  private final File serverWorkingDirectory;
  private final int heapInM;
  private final int debugPort;
  private boolean configWritten;
  private boolean hasCreatedProcess;

  public ServerInstallation(GalvanStateInterlock stateInterlock, ITestStateManager stateManager, VerboseManager stripeVerboseManager, String serverName, File serverWorkingDirectory, int heapInM, int debugPort) {
    this.stateInterlock = stateInterlock;
    this.stateManager = stateManager;
    this.stripeVerboseManager = stripeVerboseManager;
    this.serverName = serverName;
    this.serverWorkingDirectory = serverWorkingDirectory;
    this.heapInM = heapInM;
    this.debugPort = debugPort;
  }

  public void overwriteConfig(String config) throws IOException {
    File installPath = this.serverWorkingDirectory;
    File configPath = new File(installPath, "tc-config.xml");
    File oldConfigPath = new File(installPath, "tc-config.xml-old");
    configPath.renameTo(oldConfigPath);
    FileOutputStream stream = new FileOutputStream(configPath);
    byte[] toWrite = config.getBytes();
    stream.write(toWrite);
    stream.close();
    
    // Record that we are now ready to start.
    this.configWritten = true;
  }

  /**
   * @return A new ServerProcess which has not yet been started.
   */
  public ServerProcess createNewProcess() {
    // Assert that the config has been written to the installation (so it is complete).
    Assert.assertTrue(this.configWritten);
    // Assert that there isn't already a process running in this location.
    Assert.assertFalse(this.hasCreatedProcess);
    
    // Create the VerboseManager for the instance.
    VerboseManager serverVerboseManager = this.stripeVerboseManager.createComponentManager("[" + this.serverName + "]");
    // Create the process and check it out.
    ServerProcess process = new ServerProcess(this.stateInterlock, this.stateManager, serverVerboseManager, this, this.serverName, this.serverWorkingDirectory, this.heapInM, this.debugPort);
    this.hasCreatedProcess = true;
    return process;
  }
}
