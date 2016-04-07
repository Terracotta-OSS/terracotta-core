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
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.ILogger;


/**
 * Handles the description, installation, and start-up of a stripe of servers in a cluster.
 */
public class StripeInstaller {
  private final ILogger stripeLogger;
  private final ILogger fileHelperLogger;
  private final String stripeInstallDirectory;
  private final String kitOriginDirectory;
  private final List<String> extraJarPaths;
  private final List<ServerInstallation> installedServers;
  private boolean isBuilt;
  
  public StripeInstaller(ILogger stripeLogger, ILogger fileHelperLogger, String stripeInstallDirectory, String kitOriginDirectory, List<String> extraJarPaths) {
    this.stripeLogger = stripeLogger;
    this.fileHelperLogger = fileHelperLogger;
    this.stripeInstallDirectory = stripeInstallDirectory;
    this.kitOriginDirectory = kitOriginDirectory;
    this.extraJarPaths = extraJarPaths;
    
    this.installedServers = new Vector<ServerInstallation>();
  }
  
  public void installNewServer(String serverName) throws IOException {
    Assert.assertFalse(this.isBuilt);
    String installPath = FileHelpers.createTempCopyOfDirectory(this.fileHelperLogger, this.stripeInstallDirectory, serverName, this.kitOriginDirectory);
    FileHelpers.copyJarsToServer(this.fileHelperLogger, installPath, this.extraJarPaths);
    ServerInstallation installation = new ServerInstallation(this.stripeLogger, serverName, new File(installPath));
    installation.openStandardLogFiles();
    this.installedServers.add(installation);
  }
  
  public void installConfig(String configText) throws IOException {
    Assert.assertFalse(this.isBuilt);
    for (ServerInstallation installation : this.installedServers) {
      installation.overwriteConfig(configText);
    }
  }
  
  public void startServers(SynchronousProcessControl control) {
    Assert.assertFalse(this.isBuilt);
    for (ServerInstallation installation : this.installedServers) {
      control.addServerAndStart(installation);
    }
    this.isBuilt = true;
  }
}
