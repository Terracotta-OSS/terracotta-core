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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.terracotta.testing.logging.ContextualLogger;


/**
 * A helper to install, configure, and start a single stripe, along with read-only data describing how to interact with it.
 */
public class ReadyStripe {
  public static ReadyStripe configureAndStartStripe(ITestStateManager stateManager, ContextualLogger stripeLogger, ContextualLogger fileHelperLogger, String serverInstallDirectory, String testParentDirectory, int serversToCreate, int serverStartPort, int serverStartNumber, boolean isRestartable, List<String> extraJarPaths, String namespaceFragment, String serviceFragment) throws IOException, FileNotFoundException {
    ContextualLogger configLogger = new ContextualLogger(stripeLogger, "[ConfigBuilder] ");
    // Create the config builder.
    ConfigBuilder configBuilder = ConfigBuilder.buildStartPort(configLogger, serverStartPort);
    // Set fixed config details.
    configBuilder.setNamespaceSnippet(namespaceFragment);
    configBuilder.setServiceSnippet(serviceFragment);
    if (isRestartable) {
      configBuilder.setRestartable();
    }
    // Create the stripe installer.
    StripeInstaller installer = new StripeInstaller(fileHelperLogger, testParentDirectory, serverInstallDirectory, extraJarPaths);
    // Configure and install each server in the stripe.
    for (int i = 0; i < serversToCreate; ++i) {
      String serverName = "testServer" + (i + serverStartNumber);
      configBuilder.addServer(serverName);
      installer.installNewServer(serverName);
    }
    // The config is built and stripe has been installed so write the config to the stripe.
    installer.installConfig(configBuilder.buildConfig());
    
    // Create the process control object.
    ContextualLogger processControlLogger = new ContextualLogger(stripeLogger, "[ProcessControl] ");
    SynchronousProcessControl processControl = new SynchronousProcessControl(stateManager, processControlLogger);
    // Register the stripe into it and start up the server in the stripe.
    installer.startServers(processControl);
    String connectUri = configBuilder.buildUri();
    return new ReadyStripe(processControl, connectUri);
  }
  
  
  public final IMultiProcessControl stripeControl;
  public final String stripeUri;
  
  private ReadyStripe(IMultiProcessControl stripeControl, String stripeUri) {
    this.stripeControl = stripeControl;
    this.stripeUri = stripeUri;
  }
}
