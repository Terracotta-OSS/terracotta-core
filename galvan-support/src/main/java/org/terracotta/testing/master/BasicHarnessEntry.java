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

import org.terracotta.testing.api.BasicTestClusterConfiguration;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.VerboseManager;


public class BasicHarnessEntry extends AbstractHarnessEntry<BasicTestClusterConfiguration> {
  // Run the one configuration.
  @Override
  protected void runOneConfiguration(TestStateManager stateManager, VerboseManager verboseManager, DebugOptions debugOptions, CommonHarnessOptions harnessOptions, BasicTestClusterConfiguration runConfiguration) throws IOException, GalvanFailureException {
    int serversToCreate = runConfiguration.serversInStripe;
    Assert.assertTrue(serversToCreate > 0);
    
    CommonIdioms.StripeConfiguration stripeConfiguration = new CommonIdioms.StripeConfiguration();
    stripeConfiguration.kitOriginPath = harnessOptions.kitOriginPath;
    stripeConfiguration.testParentDirectory = harnessOptions.configTestDirectory;
    stripeConfiguration.serversToCreate = serversToCreate;
    stripeConfiguration.serverStartPort = chooseRandomPort();
    stripeConfiguration.serverDebugPortStart = debugOptions.serverDebugPortStart;
    stripeConfiguration.serverStartNumber = 0;
    stripeConfiguration.isRestartable = harnessOptions.isRestartable;
    stripeConfiguration.extraJarPaths = harnessOptions.extraJarPaths;
    stripeConfiguration.namespaceFragment = harnessOptions.namespaceFragment;
    stripeConfiguration.serviceFragment = harnessOptions.serviceFragment;
    stripeConfiguration.entityFragment = harnessOptions.entityFragment;
    // This is the simple case of a single-stripe so we don't need to wrap or decode anything.
    stripeConfiguration.stripeName = "stripe" + 0;
    
    ReadyStripe oneStripe = CommonIdioms.setupConfigureAndStartStripe(stateManager, verboseManager, stripeConfiguration);
    // We just want to unwrap this, directly.
    IMultiProcessControl processControl = oneStripe.stripeControl;
    String connectUri = oneStripe.stripeUri;
    Assert.assertTrue(null != processControl);
    Assert.assertTrue(null != connectUri);
    
    // Register to shut down the process control (the servers in the stripe) once the test has passed/failed.
    // Note that we want to shut down servers last.
    stateManager.addComponentToShutDown(new IComponentManager() {
      @Override
      public void forceTerminateComponent() {
        try {
          processControl.terminateAllServers();
        } catch (GalvanFailureException e) {
          // TODO:  This is just a stop-gap during refactoring.  We don't expect failure here.
          Assert.unexpected(e);
        }
      }}, false);
    
    // The cluster is now running so install and run the clients.
    CommonIdioms.ClientsConfiguration clientsConfiguration = new CommonIdioms.ClientsConfiguration();
    clientsConfiguration.testParentDirectory = harnessOptions.configTestDirectory;
    clientsConfiguration.clientClassPath = harnessOptions.clientClassPath;
    clientsConfiguration.clientsToCreate = harnessOptions.clientsToCreate;
    clientsConfiguration.clientArgumentBuilder = new BasicClientArgumentBuilder(harnessOptions.testClassName);
    clientsConfiguration.connectUri = connectUri;
    clientsConfiguration.setupClientDebugPort = debugOptions.setupClientDebugPort;
    clientsConfiguration.destroyClientDebugPort = debugOptions.destroyClientDebugPort;
    clientsConfiguration.testClientDebugPortStart = debugOptions.testClientDebugPortStart;
    CommonIdioms.installAndRunClients(stateManager, verboseManager, clientsConfiguration, processControl);
    // NOTE:  waitForFinish() throws GalvanFailureException on failure.
    stateManager.waitForFinish();
  }
}
