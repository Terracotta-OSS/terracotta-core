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

import java.io.IOException;
import java.util.List;

import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;


/**
 * A helper to install, configure, and start a single stripe, along with read-only data describing how to interact with it.
 */
public class ReadyStripe {
  /**
   * Installs, configures, and starts the described stripe.
   * When this call returns, all the servers in the stripe will be installed an running (at least in an "unknownRunning"
   *  state within the given interlock.
   * 
   * @param interlock Coordinates the relationship between the test and its inferior processes.
   * @param stateManager The object where test externals can wait for the result of a test.
   * @param stripeVerboseManager Controls verbose output for the servers within the stripe.
   * @param serverInstallDirectory The top-level directory under which all servers are installed.
   * @param kitOriginDirectory The location where the clean kit is installed.
   * @param serversToCreate The number of servers to install and start within the stripe.
   * @param heapInM The heap size to specify to the server JVM (both -Xms and -Xmx).
   * @param serverStartPort The port where servers will start being assigned.  Each server is assigned a port after the
   *  last, starting at this number.
   * @param serverDebugPortStart The port where servers will start looking for debug connections.  Each server is assigned a
   *  port after the last, starting at this number.  0 means "no debug".
   * @param serverStartNumber The server number to use.  This is so different stripes or configs don't collide with each
   *  other on disk.
   * @param isRestartable True if the servers in the stripe are restartable.
   * @param extraJarPaths The full paths to additional jars which need to be installed in each server.
   * @param namespaceFragment The namespace declaration string which must be injected into each config.
   * @param serviceFragment The service definition string which must be injected into each config.
   * @param entityFragment The static/built-in entity definition string which must be injected into each config.
   * @return The objects required to interact with and control the stripe.
   * @throws IOException Thrown in case something went wrong during server installation.
   * @throws GalvanFailureException Thrown in case starting the servers in the stripe experienced a failure.
   */
  public static ReadyStripe configureAndStartStripe(GalvanStateInterlock interlock, ITestStateManager stateManager, VerboseManager stripeVerboseManager, String serverInstallDirectory, String kitOriginDirectory, int serversToCreate, int heapInM, int serverStartPort, int serverDebugPortStart, int serverStartNumber, boolean isRestartable, List<String> extraJarPaths, String namespaceFragment, String serviceFragment, String entityFragment) throws IOException, GalvanFailureException {
    ContextualLogger configLogger = stripeVerboseManager.createComponentManager("[ConfigBuilder]").createHarnessLogger();
    // Create the config builder.
    ConfigBuilder configBuilder = ConfigBuilder.buildStartPort(configLogger, serverStartPort);
    // Set fixed config details.
    configBuilder.setNamespaceSnippet(namespaceFragment);
    configBuilder.setServiceSnippet(serviceFragment);
    configBuilder.setEntitySnippet(entityFragment);
    if (isRestartable) {
      configBuilder.setRestartable();
    }
    // Create the stripe installer.
    StripeInstaller installer = new StripeInstaller(interlock, stateManager, stripeVerboseManager, kitOriginDirectory, serverInstallDirectory, extraJarPaths);
    // Configure and install each server in the stripe.
    for (int i = 0; i < serversToCreate; ++i) {
      String serverName = "testServer" + (i + serverStartNumber);
      // Determine if we want a debug port.
      int debugPort = (serverDebugPortStart > 0)
          ? (serverDebugPortStart + i)
          : 0;
      configBuilder.addServer(serverName);
      installer.installNewServer(serverName, heapInM, debugPort);
    }
    // The config is built and stripe has been installed so write the config to the stripe.
    String configText = configBuilder.buildConfig();
    installer.installConfig(configText);
    
    // Create the process control object.
    ContextualLogger processControlLogger = stripeVerboseManager.createComponentManager("[ProcessControl]").createHarnessLogger();
    // Register the stripe into it and start up the server in the stripe.
    installer.startServers();
    
    // Before we return, we want to wait for all the servers in the stripe to come up.
    interlock.waitForAllServerRunning();
    
    // Also, so we don't start the test in a racy state, wait for all the now-running servers to enter a meaningful state.
    try {
      interlock.waitForAllServerReady();
    } catch (GalvanFailureException failed) {
//  failed to start normally but some later interaction with the framework should catch this
      System.err.println("Galvan cluster failed to start:" + failed.getMessage());
    }
    
    // We can now create the information required by the ReadyStripe and return control to the caller to run the test or install clients.
    SynchronousProcessControl processControl = new SynchronousProcessControl(interlock, processControlLogger);
    String connectUri = configBuilder.buildUri();
    ClusterInfo clusterInfo = configBuilder.getClusterInfo();
    return new ReadyStripe(processControl, connectUri, clusterInfo, configText);
  }
  
  
  public final IMultiProcessControl stripeControl;
  public final String stripeUri;
  public final ClusterInfo clusterInfo;
  public final String configText;
  
  private ReadyStripe(IMultiProcessControl stripeControl, String stripeUri, ClusterInfo clusterInfo, String configText) {
    this.stripeControl = stripeControl;
    this.stripeUri = stripeUri;
    this.clusterInfo = clusterInfo;
    this.configText = configText;
  }
}
