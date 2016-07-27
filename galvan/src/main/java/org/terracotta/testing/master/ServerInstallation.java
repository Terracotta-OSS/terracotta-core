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
import java.io.FileOutputStream;
import java.io.IOException;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.VerboseManager;


/**
 * The physical representation of a server installation, on disk.  Server processes can be started from the installation.
 */
public class ServerInstallation {
  private final VerboseManager stripeVerboseManager;
  private final String serverName;
  private final File serverWorkingDirectory;
  private final int debugPort;
  private boolean configWritten;
  private ServerProcess outstandingProcess;

  public ServerInstallation(VerboseManager stripeVerboseManager, String serverName, File serverWorkingDirectory, int debugPort) {
    this.stripeVerboseManager = stripeVerboseManager;
    this.serverName = serverName;
    this.serverWorkingDirectory = serverWorkingDirectory;
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
   * @param stateManager The state manager the inferior process can use to interact with the harness
   * 
   * @return A new ServerProcess which has not yet been started.
   */
  public ServerProcess createNewProcess(ITestStateManager stateManager) {
    // Assert that the config has been written to the installation (so it is complete).
    Assert.assertTrue(this.configWritten);
    // Assert that there isn't already a process running in this location.
    Assert.assertNull(this.outstandingProcess);
    
    // Create the VerboseManager for the instance.
    VerboseManager serverVerboseManager = this.stripeVerboseManager.createComponentManager("[" + this.serverName + "]");
    // Create the process and check it out.
    ServerProcess process = new ServerProcess(serverVerboseManager, stateManager, this, this.serverName, this.serverWorkingDirectory, this.debugPort);
    try {
      process.openLogs();
    } catch (FileNotFoundException e) {
      // Temporary - will be moved elsewhere during refactoring.
      Assert.unexpected(e);
    }
    this.outstandingProcess = process;
    return process;
  }

  public void retireProcess(ServerProcess process) {
    Assert.assertTrue(this.outstandingProcess == process);
    this.outstandingProcess = null;
    process.closeLogs();
  }
}
